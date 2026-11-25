package vn.edu.codelearn.platform.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

final class AuthDtos {

    private AuthDtos() {}

    record Credentials(
            @NotBlank @Email @CodePointLength(max = 254) String email,
            @NotBlank @CodePointLength(max = 128) String password) {

        @Override
        public String toString() {
            return "Credentials[redacted]";
        }
    }

    record AccountView(long id, String email, Account.Role role) {

        static AccountView from(Account account) {
            return new AccountView(account.id(), account.email(), account.role());
        }
    }

    record CsrfView(String headerName, String token) {}
}
