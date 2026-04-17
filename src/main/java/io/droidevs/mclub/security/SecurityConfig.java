package io.droidevs.mclub.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final AntPathRequestMatcher API_MATCHER = new AntPathRequestMatcher("/api/**");

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Provide JwtAuthenticationFilter if it's available; in slice tests it's often absent,
     * and we still want the SecurityFilterChain bean to be creatable.
     */
    @Bean(name = "jwtAuthenticationFilterForChain")
    public OncePerRequestFilter jwtAuthenticationFilterForChain(ObjectProvider<JwtAuthenticationFilter> provider) {
        JwtAuthenticationFilter filter = provider.getIfAvailable();
        if (filter != null) {
            return filter;
        }
        // no-op fallback
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                            jakarta.servlet.http.HttpServletResponse response,
                                            jakarta.servlet.FilterChain filterChain)
                    throws jakarta.servlet.ServletException, java.io.IOException {
                filterChain.doFilter(request, response);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, OncePerRequestFilter jwtAuthenticationFilterForChain) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), API_MATCHER)
                        .defaultAccessDeniedHandlerFor((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("text/plain;charset=UTF-8");
                            response.getWriter().write("Forbidden");
                        }, API_MATCHER)
                )
                .authorizeHttpRequests(auth -> auth
                        // Static
                        .requestMatchers("/css/**", "/js/**", "/error", "/favicon.ico").permitAll()

                        // Auth pages/actions
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/login", "/register").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/login", "/register", "/logout").permitAll()

                        // Public browsing pages (OPTIONAL: if you want these to require login, remove them)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/clubs", "/events").permitAll()

                        // API auth
                        .requestMatchers("/api/auth/**").permitAll()

                        // Public read-only APIs used by UI + tests
                        .requestMatchers(org.springframework.http.HttpMethod.GET,
                                "/api/events/club/*",
                                "/api/events/*/registrations/summary",
                                "/api/events/*/ratings/summary",
                                "/api/comments/**").permitAll()

                        // Home/dashboard: requires auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/").authenticated()

                        // Event detail page should be viewable when logged in (and is the redirect target after registration)
                        .requestMatchers("/events/*").authenticated()

                        // Web actions
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/events/*/register").hasRole("STUDENT")

                        // club-admin pages are restricted by membership (club-scoped). Require login here; deeper checks happen in controller.
                        .requestMatchers("/club-admin/**").authenticated()

                        // Students can apply to create a club
                        .requestMatchers("/club-applications/apply", "/club-applications/apply-club", "/club-applications/submit").authenticated()
                        .requestMatchers("/club-applications/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("PLATFORM_ADMIN")
                        .anyRequest().authenticated()
                );

        http.addFilterBefore(jwtAuthenticationFilterForChain, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
