package vn.edu.codelearn.platform.common;

import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

import jakarta.servlet.DispatcherType;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
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
    private static final String COURSE_ADMIN_API = "/api/v1/admin/courses/**";
    private static final PathPatternRequestMatcher COURSE_ADMIN = pathPattern(COURSE_ADMIN_API);
    private static final PathPatternRequestMatcher PROFILE =
            pathPattern(HttpMethod.GET, AUTH_API + "/me");
    private static final PathPatternRequestMatcher SUBMISSION_CREATE =
            pathPattern(HttpMethod.POST, "/api/v1/exercises/{exerciseId}/submissions");
    private static final PathPatternRequestMatcher SUBMISSION_READ =
            pathPattern(HttpMethod.GET, "/api/v1/submissions/{submissionId}");
    private static final PathPatternRequestMatcher SUBMISSION_DETAIL =
            pathPattern(HttpMethod.GET, "/api/v1/submissions/{submissionId}/detail");
    private static final PathPatternRequestMatcher SUBMISSION_HISTORY =
            pathPattern(HttpMethod.GET, "/api/v1/exercises/{exerciseId}/submissions");

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
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
                                                                requiresAuthentication(request)
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
                        "/api/v1/courses/{courseSlug}/lessons/{lessonSlug}/exercises",
                        "/api/v1/exercises/{exerciseId}",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness",
                        AUTH_API + "/csrf")
                .permitAll()
                .requestMatchers(HttpMethod.POST, AUTH_API + "/register", AUTH_API + "/login")
                .anonymous()
                .requestMatchers(PROFILE)
                .authenticated()
                .requestMatchers(COURSE_ADMIN)
                .hasRole("ADMIN")
                .requestMatchers(SUBMISSION_CREATE)
                .hasAnyRole("LEARNER", "AUTHOR", "ADMIN")
                .requestMatchers(SUBMISSION_READ, SUBMISSION_DETAIL, SUBMISSION_HISTORY)
                .hasAnyRole("LEARNER", "AUTHOR", "ADMIN")
                .anyRequest()
                .denyAll();
    }

    private boolean requiresAuthentication(jakarta.servlet.http.HttpServletRequest request) {
        return PROFILE.matches(request)
                || COURSE_ADMIN.matches(request)
                || SUBMISSION_CREATE.matches(request)
                || SUBMISSION_READ.matches(request)
                || SUBMISSION_DETAIL.matches(request)
                || SUBMISSION_HISTORY.matches(request);
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

        var authenticated = credentialedCors(config, List.of("GET", "POST"));
        var courseAdmin = credentialedCors(config, List.of("GET", "POST", "PUT"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(AUTH_API + "/**", authenticated);
        source.registerCorsConfiguration(COURSE_ADMIN_API, courseAdmin);
        source.registerCorsConfiguration("/api/v1/exercises/*/submissions", authenticated);
        source.registerCorsConfiguration("/api/v1/submissions/**", authenticated);
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }

    private CorsConfiguration credentialedCors(CorsConfiguration defaults, List<String> methods) {
        var config = new CorsConfiguration(defaults);
        config.setAllowedMethods(methods);
        config.setAllowedHeaders(List.of("Content-Type", "X-CSRF-TOKEN"));
        config.setAllowCredentials(true);
        config.validateAllowCredentials();
        return config;
    }
}
