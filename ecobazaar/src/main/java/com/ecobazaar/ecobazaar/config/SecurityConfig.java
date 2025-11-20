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
            
         // Custom JSON error responses for better frontend handling
            .exceptionHandling(ex -> ex
            		.authenticationEntryPoint((req, res, authEx) -> {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\": \"Unauthorized - Please log in\"}");
                    })
            		.accessDeniedHandler((req, res, accessEx) -> {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType("application/json");
                        res.getWriter().write("{\"error\": \"Forbidden - Insufficient permissions\"}");
                    })
                )
         
            // Authorize requests by path and role
            .authorizeHttpRequests(auth -> auth

            		 // Public endpoints - NO AUTH REQUIRED
                    .requestMatchers("/api/auth/**").permitAll()                              // login, register, refresh
                    .requestMatchers("/error", "/actuator/**").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                    
                 // Public static files
                    .requestMatchers("/uploads/**").permitAll()
                    
                 // QR CODE & PUBLIC VERIFICATION - ANYONE CAN SCAN
                    .requestMatchers("/api/verify/**").permitAll()
                    .requestMatchers("/api/products/*/qrcode/download").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/by-uuid/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/{id}/public").permitAll()

                    // Public product listing (for marketplace or search if any)
                    .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()

                    // Role-based access
                    .requestMatchers("/api/products/upload", "/api/products/**")
                        .hasAnyRole("FARMER", "DISTRIBUTOR", "RETAILER", "ADMIN")

                    // 🚚 Tracking endpoints — only DISTRIBUTER, RETAILER, ADMIN
                    .requestMatchers("/api/track/**")
                        .hasAnyRole("DISTRIBUTOR", "RETAILER", "ADMIN","FARMER") // Farmers can also add notes if needed

                    // 🧑‍💼 Admin-only endpoints
                    .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                     // Everything else requires authentication
                        .anyRequest().authenticated()
            )

            
            // Add our JWT filter before Spring’s default login filter
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}