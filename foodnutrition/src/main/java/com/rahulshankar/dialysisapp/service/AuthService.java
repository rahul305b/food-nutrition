package com.rahulshankar.dialysisapp.service;

public class AuthService {
    private static final String DUMMY_USERNAME = "admin";
    private static final String DUMMY_PASSWORD = "password";

    public boolean authenticate(String username, String password) {
        return DUMMY_USERNAME.equals(username) && DUMMY_PASSWORD.equals(password);
    }
}
