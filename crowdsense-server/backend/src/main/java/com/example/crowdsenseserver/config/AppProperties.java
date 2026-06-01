package com.example.crowdsenseserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private Upload upload = new Upload();
    private Inference inference = new Inference();

    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public Inference getInference() { return inference; }
    public void setInference(Inference inference) { this.inference = inference; }

    public static class Upload {
        private String dir = "uploads/images";
        private String densityDir = "uploads/density";
        private String videoDir = "uploads/videos";
        private String framesDir = "uploads/frames";

        public String getDir() { return dir; }
        public void setDir(String dir) { this.dir = dir; }
        public String getDensityDir() { return densityDir; }
        public void setDensityDir(String densityDir) { this.densityDir = densityDir; }
        public String getVideoDir() { return videoDir; }
        public void setVideoDir(String videoDir) { this.videoDir = videoDir; }
        public String getFramesDir() { return framesDir; }
        public void setFramesDir(String framesDir) { this.framesDir = framesDir; }
    }

    public static class Inference {
        private int frameInterval = 2;

        public int getFrameInterval() { return frameInterval; }
        public void setFrameInterval(int frameInterval) { this.frameInterval = frameInterval; }
    }

}
