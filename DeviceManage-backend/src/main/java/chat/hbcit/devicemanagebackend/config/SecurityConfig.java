package chat.hbcit.devicemanagebackend.config;

import chat.hbcit.devicemanagebackend.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security配置类
 *
 * @author DeviceManage
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private CustomAccessDeniedHandler customAccessDeniedHandler;

    @Autowired
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用CSRF（前后端分离，使用JWT）
                .csrf(csrf -> csrf.disable())
                // 启用CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 设置Session策略为无状态（使用JWT）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // 允许登录接口匿名访问
                        .requestMatchers("/auth/login").permitAll()
                        // 用户端接口：已认证用户（USER、ADMIN、ROOT）都可以访问
                        .requestMatchers("/categories", "/devices", "/borrow/**", "/user/**").authenticated()
                        // 管理员接口需要ADMIN或ROOT角色
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "ROOT")
                        // 其他接口需要认证
                        .anyRequest().authenticated()
                )
                // 添加JWT过滤器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                // 配置异常处理
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS配置
     * 允许所有来源的跨域请求，避免CORS问题
     *
     * @return CORS配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许所有来源（使用通配符模式）
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        // 允许的HTTP方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 允许暴露的响应头
        configuration.setExposedHeaders(Arrays.asList("*"));
        // 预检请求的有效期（秒）
        configuration.setMaxAge(3600L);
        // 允许携带凭证（Cookies、Authorization等）
        configuration.setAllowCredentials(true);
        // 注意：使用 setAllowedOriginPatterns("*") 可以与 setAllowCredentials(true) 共存

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

