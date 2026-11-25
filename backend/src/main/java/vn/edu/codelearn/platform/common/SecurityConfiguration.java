package vn.edu.codelearn.platform.common;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    private static final String AUTH_API = "/api/v1/auth";
    private static final PathPatternRequestMatcher PROFILE =
            pathPattern(HttpMethod.GET, AUTH_API + "/me");

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver,
            @Value("${app.cors.allowed-origins:}") List<String> allowedOrigins,
            CsrfTokenRepository csrfTokens,
            SecurityContextRepository contexts)
            throws Exception {
        return http.cors(cors -> cors.configurationSource(corsSource(allowedOrigins)))
                .csrf(csrf -> csrf.csrfTokenRepository(csrfTokens))
                .securityContext(context -> context.securityContextRepository(contexts))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(AbstractHttpConfigurer::disable)
                .logout(
                        logout ->
                                logout.logoutRequestMatcher(
                                                pathPattern(HttpMethod.POST, AUTH_API + "/logout"))
                                        .logoutSuccessHandler(
                                                new HttpStatusReturningLogoutSuccessHandler(
                                                        HttpStatus.NO_CONTENT)))
                .exceptionHandling(
                        errors ->
                                errors.accessDeniedHandler(
                                                (request, response, exception) ->
                                                        exceptionResolver.resolveException(
                                                                request, response, null, exception))
                                        .authenticationEntryPoint(
                                                (request, response, exception) ->
                                                        exceptionResolver.resolveException(
                                                                request,
                                                                response,
                                                                null,
                                                                PROFILE.matches(request)
                                                                        ? new ResponseStatusException(
                                                                                HttpStatus
                                                                                        .UNAUTHORIZED)
                                                                        : new AccessDeniedException(
                                                                                "Access denied",
                                                                                exception))))
                .authorizeHttpRequests(this::authorizeRequests)
                .build();
    }

    private void authorizeRequests(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry
                    requests) {
        requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                .permitAll()
                .requestMatchers(
                        HttpMethod.GET,
                        "/api/v1/health",
                        "/api/v1/courses",
                        "/api/v1/courses/{courseSlug}",
                        "/api/v1/courses/{courseSlug}/lessons/{lessonSlug}",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness",
                        AUTH_API + "/csrf")
                .permitAll()
                .requestMatchers(HttpMethod.POST, AUTH_API + "/register", AUTH_API + "/login")
                .anonymous()
                .requestMatchers(PROFILE)
                .authenticated()
                .anyRequest()
                .denyAll();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return new HttpSessionCsrfTokenRepository();
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokens) {
        return new CompositeSessionAuthenticationStrategy(
                List.of(
                        new ChangeSessionIdAuthenticationStrategy(),
                        new CsrfAuthenticationStrategy(csrfTokens)));
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository());
    }

    private UrlBasedCorsConfigurationSource corsSource(List<String> origins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET"));
        config.setExposedHeaders(List.of(RequestIdFilter.HEADER));

        CorsConfiguration auth = new CorsConfiguration(config);
        auth.setAllowedMethods(List.of("GET", "POST"));
        auth.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        auth.setAllowCredentials(true);
        auth.validateAllowCredentials();

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(AUTH_API + "/**", auth);
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }
}
