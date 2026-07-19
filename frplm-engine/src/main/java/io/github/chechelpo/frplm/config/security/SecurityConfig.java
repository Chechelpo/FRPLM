package io.github.chechelpo.frplm.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().permitAll()
                )
                .csrf(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable);

        return http.build();
    }

    /**
     * Prevents Spring Boot from creating its default development user and
     * printing a generated password during startup.
     *
     * This manager fails closed if an authentication mechanism is accidentally
     * enabled later.
     */
    @Bean
    AuthenticationManager authenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException(
                    "Authentication is not supported by this application"
            );
        };
    }
}