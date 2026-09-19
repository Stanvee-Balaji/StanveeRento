package com.example.demo.util;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class PasswordUtil {

    public String encode(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}