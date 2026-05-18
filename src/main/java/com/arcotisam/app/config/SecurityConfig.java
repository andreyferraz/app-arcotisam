package com.arcotisam.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import com.arcotisam.app.model.Usuario;
import com.arcotisam.app.repository.UsuarioRepository;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private static final String LOGIN_PATH = "/login";
    private static final String ROLE_ADMIN_MASTER = "ADMIN_MASTER";
    private static final String ROLE_ARTESAO = "ARTESAO";

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers(
                        "/",
                        "/index",
                        "/index.html",
                        "/home",
                        "/sobre",
                        "/loja",
                        "/associados",
                        "/contato",
                        "/carrinho",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/uploads/**",
                        "/favicon.ico",
                        LOGIN_PATH
                    ).permitAll()
                    // Admin-only endpoints for managing multiple artesãos
                    .requestMatchers(HttpMethod.POST, "/artesaos/**").hasRole(ROLE_ADMIN_MASTER)
                    .requestMatchers(HttpMethod.DELETE, "/artesaos/**").hasRole(ROLE_ADMIN_MASTER)
                    // Artesão endpoints: allow POST/DELETE for authenticated artesãos and admins
                    .requestMatchers(HttpMethod.POST, "/artesao/**").hasAnyRole(ROLE_ADMIN_MASTER, ROLE_ARTESAO)
                    .requestMatchers(HttpMethod.DELETE, "/artesao/**").hasAnyRole(ROLE_ADMIN_MASTER, ROLE_ARTESAO)
                    .requestMatchers("/admin", "/admin/**").hasRole(ROLE_ADMIN_MASTER)
                    .requestMatchers("/artesao", "/artesao/**").hasAnyRole(ROLE_ADMIN_MASTER, ROLE_ARTESAO)
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                    .loginPage(LOGIN_PATH)
                    .loginProcessingUrl(LOGIN_PATH)
                    .successHandler(authenticationSuccessHandler())
                    .failureUrl("/login?error=true")
                    .permitAll()
                )
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout=true")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID")
                    .permitAll()
                )
                .rememberMe(Customizer.withDefaults());

            return http.build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build security filter chain.", ex);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
        public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> usuarioRepository.findByUsername(username)
            .map(this::toUserDetails)
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
    return (request, response, authentication) -> {
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + ROLE_ADMIN_MASTER));
        boolean isArtesao = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + ROLE_ARTESAO));
        if (isAdmin) {
            response.sendRedirect("/admin");
        } else if (isArtesao) {
            response.sendRedirect("/artesao");
        } else {
            response.sendRedirect("/");
        }
    };
    }

            private org.springframework.security.core.userdetails.UserDetails toUserDetails(Usuario usuario) {
            String roleName = ROLE_ARTESAO;
            if (usuario.getRole() != null) {
                // Role enum values are like ROLE_ADMIN_MASTER, ROLE_ARTESAO
                roleName = usuario.getRole().name().replace("ROLE_", "");
            }

            return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(roleName)
                .build();
            }

}
