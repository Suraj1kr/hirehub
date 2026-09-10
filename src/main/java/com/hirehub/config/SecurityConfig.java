package com.hirehub.config;

import com.hirehub.dao.UserDao;
import java.util.Locale;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  UserDetailsService userDetailsService(UserDao users) {
    return email ->
        users
            .findByEmail(email.trim().toLowerCase(Locale.ROOT))
            .map(
                u ->
                    User.withUsername(u.getEmail())
                        .password(u.getPasswordHash())
                        .roles(u.getRole().name())
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/jobs",
                        "/jobs/*",
                        "/login",
                        "/register",
                        "/css/**",
                        "/error",
                        "/api/csrf")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/jobs", "/api/jobs/*")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/auth/register")
                    .permitAll()
                    .requestMatchers("/admin/**", "/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/applications/**", "/api/applications/**")
                    .hasRole("USER")
                    .anyRequest()
                    .authenticated())
        .formLogin(login -> login.loginPage("/login").defaultSuccessUrl("/jobs", true).permitAll())
        .logout(
            logout ->
                logout
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID"))
        .exceptionHandling(
            e ->
                e.defaultAuthenticationEntryPointFor(
                    (req, res, ex) -> res.sendError(401), new AntPathRequestMatcher("/api/**")))
        .headers(
            h ->
                h.contentSecurityPolicy(
                    c ->
                        c.policyDirectives(
                            "default-src 'self'; style-src 'self'; form-action 'self';"
                                + " frame-ancestors 'none'; base-uri 'self'")))
        .sessionManagement(s -> s.sessionFixation(f -> f.migrateSession()));
    return http.build();
  }
}
