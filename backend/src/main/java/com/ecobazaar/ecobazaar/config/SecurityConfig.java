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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import com.ecobazaar.ecobazaar.jwt.JwtAuthenticationFilter;
import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
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

    // ✅ NEW: CORS configuration bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:4200",
            "https://farm-chain-x.vercel.app",
            "https://*.vercel.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ NEW
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
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
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // ✅ NEW
                // Public routes
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/error", "/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/api/verify/**").permitAll()
                // Product-related public GETs
                .requestMatchers("/api/products/*/qrcode/download").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/by-uuid/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/{id}/public").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/*/feedbacks").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/*/feedback").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/products/*/feedback").hasRole("CONSUMER")
                .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/**").permitAll()
                // Product management
                .requestMatchers("/api/products/upload").hasAnyRole("FARMER", "ADMIN")
                .requestMatchers("/api/products/**").hasAnyRole("FARMER", "DISTRIBUTOR", "RETAILER", "ADMIN")
                // Supply chain tracking
                .requestMatchers("/api/track/**").hasAnyRole("DISTRIBUTOR", "RETAILER", "ADMIN", "FARMER")
                // Allow consumers/farmers/retailers to request admin access
                .requestMatchers(HttpMethod.POST, "/api/admin/request-admin")
                    .hasAnyRole("CONSUMER", "FARMER", "RETAILER", "ADMIN")
                // All other admin endpoints restricted to ADMIN
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Any other route must be authenticated
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
