package com.ecobazaar.ecobazaar.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

import com.ecobazaar.ecobazaar.jwt.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)  // enables @PreAuthorize and @RolesAllowed

public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF because we use JWT, not cookies
            .csrf(csrf -> csrf.disable())
            
         // Return JSON 401/403 so frontend can read the exact reason
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, authException) -> {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Unauthorized\"}");
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"error\":\"Forbidden\"}");
                    })
                )

         // 🔒 Stateless session (JWT-based)
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
         
            // Authorize requests by path and role
            .authorizeHttpRequests(auth -> auth

            		// ✅ MUST BE FIRST to avoid 403 on register/login
                    .requestMatchers("/api/auth/**").permitAll()

                 // Allow Spring Boot default error page
                    .requestMatchers("/error").permitAll()
                    
            		// ✅ feedback route MUST come before /api/products/**
                    .requestMatchers("/api/products/*/feedback").permitAll()
                    
            		// 🌍 Public routes — open to everyone (no login required)
                    .requestMatchers(
                            "/uploads/**",                     // images, static files
                            "/api/verify/**",                  // QR scan verification (public + token-supported)
                            "/api/products/*/qrcode/download"  // QR image download
                    ).permitAll()

                 // Public product viewing for GETs
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()

                 // ✅ Product modification for roles
                    .requestMatchers("/api/products/**")
                        .hasAnyRole("FARMER", "DISTRIBUTOR", "RETAILER", "ADMIN")

                    // 🚚 Tracking endpoints — only DISTRIBUTER, RETAILER, ADMIN
                    .requestMatchers("/api/track/**")
                        .hasAnyRole("DISTRIBUTOR", "RETAILER", "ADMIN")
                        
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()


                    // 🧑‍💼 Admin-only endpoints
                    .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                    // 🔐 Everything else → must be authenticated
                    .anyRequest().authenticated()
            )

            
            // Add our JWT filter before Spring’s default login filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}