package vn.edu.codelearn.platform.identity;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
class AuthService {

    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticator;

    AuthService(
            AccountRepository accounts,
            PasswordEncoder passwords,
            AuthenticationManager authenticator) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.authenticator = authenticator;
    }

    AuthDtos.AccountView register(AuthDtos.Credentials credentials) {
        String password = credentials.password();
        if (password.codePointCount(0, password.length()) < 15) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String email = canonicalEmail(credentials.email());
        return accounts.insertLearner(email, passwords.encode(password))
                .map(AuthDtos.AccountView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));
    }

    Authentication authenticate(AuthDtos.Credentials credentials) {
        return authenticator.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        canonicalEmail(credentials.email()), credentials.password()));
    }

    @Transactional(readOnly = true)
    AuthDtos.AccountView profile(String authenticatedEmail) {
        return accounts.findByEmail(authenticatedEmail)
                .map(AuthDtos.AccountView::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    private String canonicalEmail(String email) {
        String canonical = email.toLowerCase(Locale.ROOT);
        if (canonical.codePointCount(0, canonical.length()) > 254) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return canonical;
    }
}
