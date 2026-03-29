package com.youthnightschool.config;

import com.youthnightschool.security.JwtAuthenticationFilter;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration mirroring the NestJS setup: stateless JWT authentication,
 * helmet-equivalent security headers, CORS from application properties, and
 * public endpoints matching the NestJS AuthModule exclusion list.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final AppProperties appProperties;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  public SecurityConfig(AppProperties appProperties, JwtAuthenticationFilter jwtAuthenticationFilter) {
    this.appProperties = appProperties;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Stateless session (JWT-based)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

        // Disable CSRF (stateless API, no cookies for auth)
        .csrf(AbstractHttpConfigurer::disable)

        // Disable form login
        .formLogin(AbstractHttpConfigurer::disable)

        // CORS configuration from AppProperties
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))

        // Security headers (mirrors helmet config in NestJS main.ts)
        .headers(headers -> headers
            // Content-Security-Policy
            .contentSecurityPolicy(csp -> csp
                .policyDirectives(
                    "default-src 'self'; "
                    + "style-src 'self'; "
                    + "script-src 'self'; "
                    + "img-src 'self' data: https:; "
                    + "connect-src 'self'; "
                    + "font-src 'self'; "
                    + "object-src 'none'; "
                    + "media-src 'self'; "
                    + "frame-src 'none'"))
            // HTTP Strict Transport Security: 1 year, include subdomains, preload
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .preload(true)
                .maxAgeInSeconds(31536000))
            // X-Frame-Options: DENY
            .frameOptions(fo -> fo.deny())
            // X-Content-Type-Options: nosniff
            .contentTypeOptions(cto -> {})
            // Referrer-Policy
            .referrerPolicy(rp -> rp
                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
        )

        // Public endpoints (mirrors NestJS controller public routes)
        .authorizeHttpRequests(auth -> auth
            // Auth endpoints
            .requestMatchers("/api/v1/auth/**").permitAll()
            // Health check
            .requestMatchers("/api/v1/health").permitAll()
            // Article reads (public)
            .requestMatchers("GET", "/api/v1/articles/**").permitAll()
            // Course months (public)
            .requestMatchers("GET", "/api/v1/courses/months").permitAll()
            // Course list (public)
            .requestMatchers("GET", "/api/v1/courses").permitAll()
            // Course search (public)
            .requestMatchers("GET", "/api/v1/courses/search").permitAll()
            // API root
            .requestMatchers("GET", "/api").permitAll()
            // Everything else requires authentication
            .anyRequest().authenticated()
        )

        // Add JWT filter before the standard username/password filter
        .addFilterBefore(jwtAuthenticationFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    List<String> allowedOrigins = appProperties.getAllowedOriginList();
    if (allowedOrigins.isEmpty()) {
      // In non-production, allow all origins but disable credentials
      // to avoid the wildcard+credentials security issue
      config.setAllowedOriginPatterns(List.of("*"));
      config.setAllowCredentials(false);
    } else {
      config.setAllowedOrigins(allowedOrigins);
      config.setAllowCredentials(true);
    }
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }
}
