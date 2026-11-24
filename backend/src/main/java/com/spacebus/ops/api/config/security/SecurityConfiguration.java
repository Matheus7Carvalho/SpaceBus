package com.spacebus.ops.api.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacebus.ops.api.config.exception.ResponseError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;

/**
 * Autenticação por sessão (cookie JSESSIONID HttpOnly, SameSite=Strict).
 * CSRF desabilitado: o frontend é servido na mesma origem (proxy /api) e o cookie SameSite=Strict
 * não é enviado em requisições de outros sites.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    public static final String LOGIN_URL = "/api/auth/login";
    public static final String LOGOUT_URL = "/api/auth/logout";

    /** Perfis da operação: mantêm frota, rotas e viagens da transportadora. */
    private static final String[] OPERACAO = {"ADMINISTRADOR", "OPERADOR"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository,
                                                   ObjectMapper objectMapper) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // Restrição por perfil fica aqui, por recurso. @PreAuthorize na classe do controller NÃO
                // alcança os endpoints herdados de BaseController/BaseSearchController.
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, LOGIN_URL).permitAll()
                        // Retorno do provedor de pagamento: sem sessão, autenticado pelo segredo no cabeçalho.
                        .requestMatchers(HttpMethod.POST, "/api/pagamento/webhook").permitAll()
                        .requestMatchers("/api/reserva", "/api/reserva/**").hasRole("PASSAGEIRO")
                        .requestMatchers("/api/motorista", "/api/motorista/**").hasRole("MOTORISTA")
                        .requestMatchers("/api/notificacao", "/api/notificacao/**").hasRole("PASSAGEIRO")
                        .requestMatchers("/api/veiculo", "/api/veiculo/**").hasAnyRole(OPERACAO)
                        .requestMatchers("/api/rota", "/api/rota/**").hasAnyRole(OPERACAO)
                        .requestMatchers("/api/viagem", "/api/viagem/**").hasAnyRole(OPERACAO)
                        .requestMatchers("/api/indicador", "/api/indicador/**").hasAnyRole(OPERACAO)
                        .requestMatchers("/api/usuario", "/api/usuario/**").hasAnyRole(OPERACAO)
                        .requestMatchers("/api/dev/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                write(res, objectMapper, HttpStatus.UNAUTHORIZED, "Sessão expirada. Entre novamente."))
                        .accessDeniedHandler((req, res, e) ->
                                write(res, objectMapper, HttpStatus.FORBIDDEN, "Acesso não permitido para o seu perfil.")))
                .logout(logout -> logout
                        .logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
                .build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        var provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    private static void write(HttpServletResponse response, ObjectMapper mapper, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getOutputStream(), ResponseError.builder().status(status.value()).message(message).build());
    }
}
