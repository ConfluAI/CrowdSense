"""FastAPI GPU inference server for DM-Count crowd counting."""
import base64
import io
import os
import sys
import time
import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFont
import torch
from torchvision import transforms
from fastapi import FastAPI, UploadFile, File, Query
from fastapi.responses import JSONResponse
import uvicorn

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from models import resnet_fpn

app = FastAPI(title="DM-Count Inference")

device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = None

# ImageNet normalization (same as training)
transform = transforms.Compose([
    transforms.ToTensor(),
    transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225])
])

# Jet colormap LUT
def _build_jet():
    lut = np.zeros((256, 3), dtype=np.uint8)
    for i in range(256):
        t = i / 255.0
        r = int(np.clip(_interp(t, 0, 0, 0.25, 0, 0.5, 0, 0.75, 255, 1.0, 255), 0, 255))
        g = int(np.clip(_interp(t, 0, 0, 0.25, 0, 0.5, 255, 0.75, 255, 1.0, 0), 0, 255))
        b = int(np.clip(_interp(t, 0, 128, 0.25, 255, 0.5, 255, 0.75, 0, 1.0, 0), 0, 255))
        lut[i] = [r, g, b]
    return lut

def _interp(t, t0, v0, t1, v1, t2, v2, t3, v3, t4, v4):
    if t <= t1: return v0 + (v1 - v0) * (t - t0) / (t1 - t0) if t1 > t0 else v0
    if t <= t2: return v1 + (v2 - v1) * (t - t1) / (t2 - t1) if t2 > t1 else v1
    if t <= t3: return v2 + (v3 - v2) * (t - t2) / (t3 - t2) if t3 > t2 else v2
    return v3 + (v4 - v3) * (t - t3) / (t4 - t3) if t4 > t3 else v4

JET_LUT = _build_jet()


def load_model():
    global model
    model_path = os.path.join("saved_models", "resnet_fpn_best_mae98.pth")
    model = resnet_fpn()
    model.load_state_dict(torch.load(model_path, map_location=device))
    model.to(device).eval()
    print(f"Model loaded on {device}")


def analyze_density_level(density_map):
    """Classify by local density map values — correlates with 人/m².

    Uses max pixel value after Gaussian smoothing as the crowding indicator.
    Since the density map is normalized per-pixel, max_val reflects the
    most crowded local region's density, analogous to national safety
    standard (人/m²) at the model's learned spatial scale.

    Thresholds calibrated against DM-Count output range on QNRF.
    """
    smoothed = cv2.GaussianBlur(density_map, (15, 15), 0)
    max_val = float(np.max(smoothed))

    if max_val < 0.02:
        return "低密度 Low", "green", (0, 255, 0, 255)
    elif max_val < 0.06:
        return "正常密度 Normal", "orange", (0, 255, 255, 255)
    elif max_val < 0.15:
        return "密集 Dense", "orangered", (0, 165, 255, 255)
    else:
        return "极度拥挤 Crowded", "red", (0, 0, 255, 255)


def density_to_heatmap(density, orig_w, orig_h, level_str, level_color):
    """Convert density map to jet colormap PNG with status overlay."""
    dmap = density[0, 0].cpu().numpy()
    dmap_h, dmap_w = dmap.shape

    dmin, dmax = dmap.min(), dmap.max()
    if dmax - dmin < 1e-6:
        dmax = dmin + 1.0
    norm = ((dmap - dmin) / (dmax - dmin) * 255).astype(np.uint8)
    colored = JET_LUT[norm]

    # Resize to original dimensions
    img = Image.fromarray(colored)
    if img.size != (orig_w, orig_h):
        img = img.resize((orig_w, orig_h), Image.BILINEAR)

    # Draw status overlay
    # Convert level_color from BGR to RGB
    rgb_color = (level_color[2], level_color[1], level_color[0])
    draw = ImageDraw.Draw(img)
    font_size = max(int(orig_h / 20), 16)
    try:
        font = ImageFont.truetype("simhei.ttf", font_size)
    except:
        try:
            font = ImageFont.truetype("C:/Windows/Fonts/simhei.ttf", font_size)
        except:
            font = ImageFont.load_default()
    draw.text((20, 20), f"Status: {level_str}", font=font, fill=rgb_color)
    draw.text((20, 20 + font_size + 5), f"Count: {int(dmap.sum())}", font=font, fill=(255, 255, 255))

    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue(), (dmap_h, dmap_w)


@app.on_event("startup")
def startup():
    load_model()


@app.post("/infer")
async def infer(file: UploadFile = File(...)):
    t0 = time.time()
    contents = await file.read()

    # Load and preprocess
    img = Image.open(io.BytesIO(contents)).convert("RGB")
    orig_w, orig_h = img.size

    # Resize large images for speed
    max_dim = 1024
    w, h = img.size
    if max(w, h) > max_dim:
        scale = max_dim / max(w, h)
        img = img.resize((int(w * scale), int(h * scale)), Image.BILINEAR)

    tensor = transform(img).unsqueeze(0).to(device)  # [1, 3, H, W]

    # Inference
    with torch.no_grad():
        density, _ = model(tensor)

    dmap_np = density[0, 0].cpu().numpy()
    count = int(dmap_np.sum())
    level_str, level_tag, level_bgra = analyze_density_level(dmap_np)
    heatmap_png, (dmap_h, dmap_w) = density_to_heatmap(density, orig_w, orig_h, level_str, level_bgra)
    elapsed = int((time.time() - t0) * 1000)

    heatmap_b64 = base64.b64encode(heatmap_png).decode("ascii")

    return JSONResponse({
        "crowdCount": count,
        "densityLevel": level_str,
        "levelTag": level_tag,
        "levelColor": list(level_bgra[:3]),
        "inferenceTime": elapsed,
        "heatmapBase64": heatmap_b64,
        "dmapW": dmap_w,
        "dmapH": dmap_h
    })


def _process_video(video_path, interval, batch_size, frames_dir, density_dir, task_id, cleanup_temp=False):
    """Core video processing: extract frames, batch inference, save files to disk.

    Saves frame images and density maps directly to the shared upload directories.
    Returns metadata only (file paths, counts, levels) — no base64.
    """
    t0 = time.time()

    cap = cv2.VideoCapture(video_path)
    if not cap.isOpened():
        if cleanup_temp:
            try: os.remove(video_path)
            except: pass
        return {"error": "Cannot open video file"}

    fps = cap.get(cv2.CAP_PROP_FPS)
    total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
    duration = total_frames / fps if fps > 0 else 0

    frames_subdir = os.path.join(frames_dir, str(task_id))
    os.makedirs(frames_subdir, exist_ok=True)
    os.makedirs(density_dir, exist_ok=True)

    raw_frames = []
    max_dim = 1024
    t_extract = time.time()
    t = 0.0
    while t < duration:
        cap.set(cv2.CAP_PROP_POS_MSEC, t * 1000)
        ret, frame_bgr = cap.read()
        if not ret:
            t += interval
            continue
        frame_rgb = cv2.cvtColor(frame_bgr, cv2.COLOR_BGR2RGB)
        img = Image.fromarray(frame_rgb)
        ow, oh = img.size
        if max(ow, oh) > max_dim:
            scale = max_dim / max(ow, oh)
            img = img.resize((int(ow * scale), int(oh * scale)), Image.BILINEAR)
        raw_frames.append((t, img, ow, oh))
        t += interval
    cap.release()
    if cleanup_temp:
        try: os.remove(video_path)
        except: pass
    extract_ms = int((time.time() - t_extract) * 1000)

    if not raw_frames:
        return {"error": "No frames extracted"}

    t_infer = time.time()
    all_results = []
    for chunk_start in range(0, len(raw_frames), batch_size):
        chunk = raw_frames[chunk_start:chunk_start + batch_size]
        tensors = [transform(f[1]).unsqueeze(0) for f in chunk]
        batch_tensor = torch.cat(tensors, dim=0).to(device)
        with torch.no_grad():
            densities, _ = model(batch_tensor)

        for i, (timestamp, img, orig_w, orig_h) in enumerate(chunk):
            fi = chunk_start + i
            w, h = img.size
            dmap_np = densities[i, 0].cpu().numpy()
            count = int(dmap_np.sum())
            level_str, level_tag, level_bgra = analyze_density_level(dmap_np)

            heatmap_png, _ = density_to_heatmap(
                densities[i:i + 1], orig_w, orig_h, level_str, level_bgra)

            # Save frame image to disk directly
            frame_name = f"frame_{fi:04d}.jpg"
            frame_path = os.path.join(frames_subdir, frame_name)
            img.save(frame_path, format="JPEG", quality=85)

            # Save density heatmap to disk directly
            dmap_name = f"density_{task_id}_{fi}.png"
            dmap_path = os.path.join(density_dir, dmap_name)
            with open(dmap_path, "wb") as f:
                f.write(heatmap_png)

            all_results.append({
                "frameIndex": fi,
                "timestamp": round(timestamp, 2),
                "crowdCount": count,
                "densityLevel": level_str,
                "levelTag": level_tag,
                "imagePath": f"{task_id}/{frame_name}",
                "densityPath": dmap_name,
                "width": w,
                "height": h
            })

    infer_ms = int((time.time() - t_infer) * 1000)
    per_frame = infer_ms / len(raw_frames) if raw_frames else 0
    for r in all_results:
        r["inferenceTime"] = int(per_frame)
    total_ms = int((time.time() - t0) * 1000)

    return {
        "totalFrames": len(all_results),
        "duration": round(duration, 2),
        "fps": round(fps, 2),
        "interval": interval,
        "totalTime": total_ms,
        "extractTime": extract_ms,
        "inferenceTime": infer_ms,
        "frames": all_results
    }


@app.post("/infer-video")
async def infer_video(file: UploadFile = File(...), interval: float = 2.0,
                       batch_size: int = 8, frames_dir: str = "", density_dir: str = "",
                       task_id: int = 0):
    """Extract frames from uploaded video via HTTP multipart (backward compatible)."""
    contents = await file.read()
    video_path = os.path.join(os.path.dirname(__file__), "_tmp_video.mp4")
    with open(video_path, "wb") as f:
        f.write(contents)
    fd = frames_dir or os.path.join(os.path.dirname(__file__), "_frames")
    dd = density_dir or os.path.join(os.path.dirname(__file__), "_density")
    result = _process_video(video_path, interval, batch_size, fd, dd, task_id, cleanup_temp=True)
    if "error" in result:
        return JSONResponse(result, status_code=400)
    return JSONResponse(result)


@app.get("/infer-video")
async def infer_video_path(path: str = Query(...), interval: float = 2.0,
                            batch_size: int = 8, frames_dir: str = Query(...),
                            density_dir: str = Query(...), task_id: int = Query(...)):
    """Extract frames from local video file, save results directly to shared dirs."""
    result = _process_video(path, interval, batch_size, frames_dir, density_dir, task_id)
    if "error" in result:
        return JSONResponse(result, status_code=400)
    return JSONResponse(result)


@app.get("/health")
def health():
    return {"status": "ok", "device": str(device)}


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=8000)
