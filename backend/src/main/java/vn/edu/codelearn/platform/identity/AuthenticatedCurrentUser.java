package vn.edu.codelearn.platform.identity;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class AuthenticatedCurrentUser implements CurrentUser {

    private final AccountRepository accounts;

    AuthenticatedCurrentUser(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    public long id() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return accounts.findIdByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
