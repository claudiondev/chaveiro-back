package com.chaveiro_abencoado.back.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    @Value("${cors.origins:http://localhost:5173,http://localhost:5174,http://localhost:3000}")
    private String corsOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 401: sem token, token inválido/expirado, ou sessão revogada (senha trocada).
            // 403: autenticado, mas sem o role exigido. Sem isso, o comportamento padrão
            // do Spring Security devolve 403 pros dois casos, e o front não consegue
            // diferenciar "sua sessão caiu, faça login de novo" de "você não pode fazer isso".
            .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(jsonErrorResponse(HttpStatus.UNAUTHORIZED, "Não autenticado"))
                    .accessDeniedHandler(jsonAccessDeniedResponse()))
            .authorizeHttpRequests(auth -> auth
                // Auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/cadastro").hasRole("DONO")
                .requestMatchers(HttpMethod.PUT, "/api/auth/senha").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/auth/usuarios").hasRole("DONO")
                .requestMatchers(HttpMethod.PATCH, "/api/auth/usuarios/*/status").hasRole("DONO")

                // Ajuda contextual — progresso sempre pertence ao usuário autenticado
                .requestMatchers("/api/ajuda/**").authenticated()

                // Swagger
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Tipos de Serviço
                .requestMatchers(HttpMethod.GET, "/api/tipos-servico/**").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/tipos-servico/**").hasRole("DONO")
                .requestMatchers(HttpMethod.PUT, "/api/tipos-servico/**").hasRole("DONO")
                .requestMatchers(HttpMethod.DELETE, "/api/tipos-servico/**").hasRole("DONO")

                // Caixa — fechamento e histórico só pro dono
                .requestMatchers(HttpMethod.POST, "/api/caixa/fechamento").hasRole("DONO")
                .requestMatchers(HttpMethod.GET, "/api/caixa/historico", "/api/caixa/historico/**").hasRole("DONO")
                .requestMatchers("/api/caixa/**").authenticated()

                // Relatórios — só dono
                .requestMatchers("/api/relatorios/**").hasRole("DONO")

                // Serviços — pendentes e pagar só dono
                .requestMatchers(HttpMethod.GET, "/api/servicos/pendentes").hasRole("DONO")
                .requestMatchers(HttpMethod.PATCH, "/api/servicos/*/pagar").authenticated()
                .requestMatchers("/api/servicos/**").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private AuthenticationEntryPoint jsonErrorResponse(HttpStatus status, String mensagemPadrao) {
        return (request, response, authException) -> escreverErroJson(response, status, mensagemPadrao);
    }

    private AccessDeniedHandler jsonAccessDeniedResponse() {
        return (request, response, accessDeniedException) ->
                escreverErroJson(response, HttpStatus.FORBIDDEN, "Sem permissão para este recurso");
    }

    private void escreverErroJson(jakarta.servlet.http.HttpServletResponse response, HttpStatus status,
                                  String mensagem) throws IOException {
        response.setStatus(status.value());
        // Sem isso, acentos saem corrompidos: response.getWriter() usa o charset da
        // resposta, e sem setCharacterEncoding ele não é necessariamente UTF-8.
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> corpo = Map.of(
                "erro", mensagem,
                "status", status.value(),
                "timestamp", LocalDateTime.now().toString()
        );
        new ObjectMapper().writeValue(response.getWriter(), corpo);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
