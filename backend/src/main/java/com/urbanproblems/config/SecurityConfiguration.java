
package com.urbanproblems.config;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.urbanproblems.security.JwtAuthenticationFilter;
import com.urbanproblems.security.JwtTokenProvider;
import com.urbanproblems.security.SessionAccountStatusFilter;
import com.urbanproblems.service.AccountStatusService;
import com.urbanproblems.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    DaoAuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        // Spring Security 7 constructor-based initialization
        DaoAuthenticationProvider authProvider =
                new DaoAuthenticationProvider(userDetailsService);

        authProvider.setPasswordEncoder(passwordEncoder);

        return authProvider;
    }

    @Bean
    AuthenticationManager authenticationManager(
            HttpSecurity http,
            DaoAuthenticationProvider authenticationProvider) throws Exception {

        AuthenticationManagerBuilder builder =
                http.getSharedObject(AuthenticationManagerBuilder.class);

        builder.authenticationProvider(authenticationProvider);

        return builder.build();
    }

    @Bean
    @Profile("test")
    JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenProvider tokenProvider,
            AccountStatusService accountStatusService) {

        return new JwtAuthenticationFilter(tokenProvider, accountStatusService);
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CookieSerializer sessionCookieSerializer(
            @Value("${server.servlet.session.cookie.name:CGSESSION}") String cookieName,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure,
            @Value("${server.servlet.session.cookie.same-site:lax}") String sameSite) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(cookieName);
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite(sameSite);
        return serializer;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}")
            List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(
                List.of("Content-Type", "Accept", "X-XSRF-TOKEN")
        );
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    SecurityFilterChain filterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            AccountStatusService accountStatusService,
            ObjectProvider<JwtAuthenticationFilter> legacyJwtAuthenticationFilter,
            @Value("${app.security.csrf-enabled:true}") boolean csrfEnabled) throws Exception {

        http
            .cors(Customizer.withDefaults())

            .securityContext(context -> context.securityContextRepository(securityContextRepository))

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId()))

            .authorizeHttpRequests(authz -> authz
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/register/worker",
                        "/api/auth/departments", "/api/auth/csrf",
                        "/api/auth/forgot-password/question", "/api/auth/forgot-password/reset").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/logout").authenticated()
                .requestMatchers("/api/system/health/**").permitAll()
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeJsonError(
                        response,
                        HttpServletResponse.SC_UNAUTHORIZED,
                        "UNAUTHORIZED",
                        "Authentication required"
                    )
                )

                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeJsonError(
                        response,
                        HttpServletResponse.SC_FORBIDDEN,
                        "FORBIDDEN",
                        "Access denied"
                    )
                )
            )

            .httpBasic(basic -> basic.disable());

        if (csrfEnabled) {
            http.csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()));
        } else {
            http.csrf(csrf -> csrf.disable());
        }

        JwtAuthenticationFilter legacyFilter = legacyJwtAuthenticationFilter.getIfAvailable();
        if (legacyFilter != null) {
            http.addFilterBefore(legacyFilter, UsernamePasswordAuthenticationFilter.class);
        }
        http.addFilterAfter(new SessionAccountStatusFilter(accountStatusService),
                org.springframework.security.web.context.SecurityContextHolderFilter.class);

        return http.build();
    }

    private static void writeJsonError(
            HttpServletResponse response,
            int status,
            String error,
            String message) throws java.io.IOException {

        response.setStatus(status);
        response.setContentType("application/json");

        response.getWriter().write(
            "{\"success\":false,\"error\":\"" + error +
            "\",\"message\":\"" + message + "\"}"
        );
    }
}
