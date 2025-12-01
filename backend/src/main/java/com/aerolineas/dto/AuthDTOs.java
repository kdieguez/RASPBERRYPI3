package com.aerolineas.dto;

public class AuthDTOs {

    public static class RegisterRequest {
        public String email;
        public String password;
        public String nombres;
        public String apellidos;
        // public String captchaToken;

        public RegisterRequest() {
        }

        public RegisterRequest(String email, String password, String nombres, String apellidos) {
            this.email = email;
            this.password = password;
            this.nombres = nombres;
            this.apellidos = apellidos;
        }
    }

    public static class LoginRequest {
        public String email;
        public String password;

        public LoginRequest() {
        }

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class UserView {
        public Long id;
        public String email;
        public String nombres;
        public String apellidos;
        public Integer idRol;

        public UserView() {
        }

        public UserView(Long id, String email, String nombres, String apellidos, Integer idRol) {
            this.id = id;
            this.email = email;
            this.nombres = nombres;
            this.apellidos = apellidos;
            this.idRol = idRol;
        }
    }

    public static class LoginResponse {
        public String token;
        public long expiresInSeconds;
        public UserView user;

        public LoginResponse() {
        }

        public LoginResponse(String token, long expiresInSeconds, UserView user) {
            this.token = token;
            this.expiresInSeconds = expiresInSeconds;
            this.user = user;
        }
    }
}
