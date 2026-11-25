package vn.edu.codelearn.platform.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
class IdentityAuthenticationConfiguration {

    @Bean
    PasswordEncoder passwordEncoder() {
        // OWASP Argon2id baseline: 19 MiB, two iterations, one lane.
        return new Argon2PasswordEncoder(16, 32, 1, 19 * 1024, 2);
    }

    @Bean
    AuthenticationManager authenticationManager(
            AccountRepository accounts, PasswordEncoder passwords) {
        UserDetailsService users =
                email ->
                        accounts.findByEmail(email)
                                .map(
                                        account ->
                                                User.withUsername(account.email())
                                                        .password(account.passwordHash())
                                                        .roles(account.role().name())
                                                        .build())
                                .orElseThrow(
                                        () -> new UsernameNotFoundException("Account not found"));
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(passwords);
        return new ProviderManager(provider);
    }
}
