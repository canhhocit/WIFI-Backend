package com.wificaptive.authentication.dto;

public class LoginResponse {
    private String token;
    private String tokenType = "Bearer";
    private AdminUserDto user;

    public LoginResponse() {
    }

    public LoginResponse(String token, AdminUserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public AdminUserDto getUser() {
        return user;
    }

    public void setUser(AdminUserDto user) {
        this.user = user;
    }
}
