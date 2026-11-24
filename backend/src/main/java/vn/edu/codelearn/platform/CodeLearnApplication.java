package vn.edu.codelearn.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CodeLearnApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodeLearnApplication.class, args);
    }
}
