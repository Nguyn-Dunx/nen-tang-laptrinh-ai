package vn.edu.codelearn.platform.identity;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

    private final AuthService auth;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;

    AuthController(
            AuthService auth,
            SessionAuthenticationStrategy sessions,
            SecurityContextRepository contexts) {
        this.auth = auth;
        this.sessions = sessions;
        this.contexts = contexts;
    }

    @GetMapping("/csrf")
    ResponseEntity<AuthDtos.CsrfView> csrf(CsrfToken token) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new AuthDtos.CsrfView(token.getHeaderName(), token.getToken()));
    }

    @PostMapping("/register")
    ResponseEntity<AuthDtos.AccountView> register(
            @Valid @RequestBody AuthDtos.Credentials credentials,
            HttpServletRequest request,
            HttpServletResponse response) {
        var account = auth.register(credentials);
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        account.email(),
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + account.role().name()));
        startSession(authentication, request, response);
        return ResponseEntity.created(URI.create("/api/v1/auth/me")).body(account);
    }

    @PostMapping("/login")
    ResponseEntity<Void> login(
            @Valid @RequestBody AuthDtos.Credentials credentials,
            HttpServletRequest request,
            HttpServletResponse response) {
        startSession(auth.authenticate(credentials), request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    AuthDtos.AccountView profile(Principal principal) {
        return auth.profile(principal.getName());
    }

    private void startSession(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        sessions.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
    }
}
