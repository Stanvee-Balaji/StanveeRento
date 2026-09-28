package com.example.demo.dto;

public class StanveeAuthDTO {

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class LoginResult {
        private final String rawJson;
        private final String jwtToken;

        public LoginResult(String rawJson, String jwtToken) {
            this.rawJson = rawJson;
            this.jwtToken = jwtToken;
        }

        public String getRawJson()  { return rawJson; }
        public String getJwtToken() { return jwtToken; }
    }
}