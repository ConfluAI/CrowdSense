package com.example.crowdsenseserver.config;

import com.example.crowdsenseserver.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            // 开启 CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 关闭 CSRF
            .csrf(csrf -> csrf.disable())

            // JWT 无状态会话
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 权限配置
            .authorizeHttpRequests(auth -> auth

                // 放行 OPTIONS 预检请求（解决 Swagger 403）
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 登录注册接口放行（同时带 /api 前缀和不带前缀，兼容不同匹配方式）
                .requestMatchers(
                        new AntPathRequestMatcher("/auth/login", "POST"),
                        new AntPathRequestMatcher("/auth/register", "POST"),
                        new AntPathRequestMatcher("/api/auth/login", "POST"),
                        new AntPathRequestMatcher("/api/auth/register", "POST")
                ).permitAll()

                // Swagger 放行
                .requestMatchers(
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**"
                ).permitAll()

                // 文件访问放行
                .requestMatchers(
                        "/files/**",
                        "/api/files/**"
                ).permitAll()

                // Spring Boot 错误路径放行（防止 JSON 解析失败时内部转发 /error 被拦回 403）
                .requestMatchers("/error").permitAll()

                // 其余请求需要认证
                .anyRequest().authenticated()
            )

            // 异常处理：未认证返回 401，权限不足返回 403
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) ->
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未认证"))
                    .accessDeniedHandler((request, response, accessDeniedException) ->
                            response.sendError(HttpServletResponse.SC_FORBIDDEN, "无权限"))
            )

            // JWT 过滤器
            .addFilterBefore(
                    jwtAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * 跨域配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        // 允许前端来源（带凭证时不能用 *，需指定具体地址）
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));

        // 允许请求方式
        config.setAllowedMethods(Arrays.asList(
                "GET",
                "POST",
                "PUT",
                "DELETE",
                "OPTIONS"
        ));

        // 允许请求头
        config.setAllowedHeaders(List.of("*"));

        // 暴露响应头
        config.setExposedHeaders(List.of("Authorization"));

        // 允许携带 Cookie
        config.setAllowCredentials(true);

        // 预检请求缓存时间
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    /**
     * 密码加密器
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config
    ) throws Exception {

        return config.getAuthenticationManager();
    }
}