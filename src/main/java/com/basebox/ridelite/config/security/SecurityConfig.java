package com.basebox.ridelite.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.basebox.ridelite.config.security.jwt.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;
import com.basebox.ridelite.config.security.jwt.JwtAuthenticationEntryPoint;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final CorsConfigurationSource corsConfigurationSource;
    

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final PasswordEncoder passwordEncoder;

    
    
    // =========================================================================
    // SECURITY FILTER CHAIN (Main Configuration)
    // =========================================================================
    
    /**
     * Configure HTTP security.
     * 
     * SECURITY RULES:
     * - Public endpoints: /api/v1/auth/** (login, register)
     * - Protected endpoints: Everything else
     * - Stateless sessions (JWT-based)
     * - CORS enabled
     * - CSRF disabled (not needed for JWT)
     * 
     * @param http HttpSecurity builder
     * @return Configured SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ─────────────────────────────────────────────────────────────
            // Disable CSRF (Cross-Site Request Forgery)
            // ─────────────────────────────────────────────────────────────
            // CSRF protection not needed for stateless JWT authentication
            // CSRF protects against form-based attacks, but we use JSON APIs
            .csrf(AbstractHttpConfigurer::disable)
            
            // ─────────────────────────────────────────────────────────────
            // Configure CORS (Cross-Origin Resource Sharing)
            // ─────────────────────────────────────────────────────────────
            // Uses CorsConfigurationSource bean from CorsConfig
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            
            // ─────────────────────────────────────────────────────────────
            // Configure authorization rules
            // ─────────────────────────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // ═════════════════════════════════════════════════════════
                // PUBLIC ENDPOINTS (no authentication required)
                // ═════════════════════════════════════════════════════════
                .requestMatchers("/api/v1/auth/**").permitAll()           // Login, register
                .requestMatchers("/swagger-ui/**").permitAll()            // Swagger UI
                .requestMatchers("/v3/api-docs/**").permitAll()           // OpenAPI docs
                .requestMatchers("/actuator/health").permitAll()          // Health check
                
                // ═════════════════════════════════════════════════════════
                // ROLE-BASED ACCESS (examples)
                // ═════════════════════════════════════════════════════════
                // Option 1: Configure here (less flexible)
                // .requestMatchers("/api/v1/drivers/**").hasRole("DRIVER")
                // .requestMatchers("/api/v1/trips/**").hasAnyRole("DRIVER", "CLIENT")
                
                // Option 2: Use @PreAuthorize in controllers (recommended)
                // We'll use this approach - see controller examples below
                
                // ═════════════════════════════════════════════════════════
                // ALL OTHER ENDPOINTS (require authentication)
                // ═════════════════════════════════════════════════════════
                .anyRequest().authenticated()
            )
            
            // ─────────────────────────────────────────────────────────────
            // Exception handling
            // ─────────────────────────────────────────────────────────────
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // ─────────────────────────────────────────────────────────────
            // Session management (STATELESS)
            // ─────────────────────────────────────────────────────────────
            // Spring won't create HTTP sessions
            // All authentication via JWT tokens
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // ─────────────────────────────────────────────────────────────
            // Authentication provider
            // ─────────────────────────────────────────────────────────────
            .authenticationProvider(authenticationProvider())
            
            // ─────────────────────────────────────────────────────────────
            // Add JWT filter before UsernamePasswordAuthenticationFilter
            // ─────────────────────────────────────────────────────────────
            // This ensures JWT is checked BEFORE default Spring Security filters
            .addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    // =========================================================================
    // AUTHENTICATION PROVIDER
    // =========================================================================
    
    /**
     * Authentication provider using our custom UserDetailsService.
     * 
     * WHAT IT DOES:
     * - Uses CustomUserDetailsService to load users
     * - Uses BCrypt to verify passwords
     * - Called during login process
     * 
     * @return Configured authentication provider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }
    
    // =========================================================================
    // AUTHENTICATION MANAGER
    // =========================================================================
    
    /**
     * Authentication manager bean.
     * 
     * USED FOR:
     * - Manual authentication (login endpoint)
     * - authenticate(username, password) method
     * 
     * @param config Authentication configuration
     * @return AuthenticationManager
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    // =========================================================================
    // PASSWORD ENCODER
    // =========================================================================
    
    /**
     * Password encoder using BCrypt.
     * 
     * WHAT IS BCRYPT?
     * - One-way hashing algorithm
     * - Built-in salt (random data)
     * - Adaptive (can increase difficulty over time)
     * 
     * NEVER STORE PLAIN PASSWORDS!
     * Plain:   "password123"           ❌ NEVER!
     * Hashed:  "$2a$10$N9qo8..."      ✅ Always!
     * 
     * BCRYPT EXAMPLE:
     * Input:  "myPassword123"
     * Output: "$2a$10$N9qo8uLOickgx2ZMRZoMy.eR.N9qo8uLOickgx2ZMRZoMy"
     * 
     * Same password, different hash each time (due to random salt):
     * Hash 1: "$2a$10$ABC..."
     * Hash 2: "$2a$10$XYZ..."
     * 
     * @return BCrypt password encoder
     */
    //@Bean
    // public PasswordEncoder passwordEncoder() {
    //     return new PasswordEncoderConfig().passwordEncoder();
    // }
    
    // @Bean
    // public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    //     http
    //         .cors(cors -> cors.configurationSource(corsConfigurationSource))
    //         .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for API testing
    //         .authorizeHttpRequests(auth -> auth
    //             // Public endpoints
    //             .requestMatchers(
    //                 "/",
    //                 "/login",
    //                 "/swagger-ui/**",
    //                 "/v3/api-docs/**",
    //                 "/swagger-ui.html",
    //                 "/webjars/**",
    //                 "/swagger-resources/**",
    //                 "/api/auth/**",
    //                 "/api/public/**"
    //             ).permitAll()
    //             // All other endpoints require authentication
    //             .anyRequest().authenticated()
    //         )
    //         .formLogin(form -> form
    //             .loginPage("/login")
    //             .loginProcessingUrl("/login")
    //             .defaultSuccessUrl("/swagger-ui/index.html", true) // Redirect to Swagger after login
    //             .failureUrl("/login?error=true")
    //             .permitAll()
    //         )
    //         .logout(logout -> logout
    //             .logoutUrl("/logout")
    //             .logoutSuccessUrl("/login?logout=true")
    //             .permitAll()
    //         )
    //         .httpBasic(httpBasic -> {}); // Enable HTTP Basic Auth for API testing
        
    //     return http.build();
    // }
    
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        // Create test users with encoded passwords
        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder.encode("admin123"))
            .roles("ADMIN")
            .build();
        
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder.encode("password"))
            .roles("USER")
            .build();
        
        UserDetails test = User.builder()
            .username("test")
            .password(passwordEncoder.encode("test123"))
            .roles("USER")
            .build();
        
        return new InMemoryUserDetailsManager(List.of(admin, user, test));
    }
}
