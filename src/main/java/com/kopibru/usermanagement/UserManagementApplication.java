package com.kopibru.usermanagement;

import com.kopibru.usermanagement.config.SchemaBootstrap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class UserManagementApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(UserManagementApplication.class);
        application.addListeners(new SchemaBootstrap());
        application.run(args);
    }
}
