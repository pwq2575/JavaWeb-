package com.ers.auth.dto;

/**
 * 登录/注册成功响应（API 契约 §2 #1、#2）。
 */
public class AuthResponse {

    private String token;
    private long expiresIn;
    private UserVO user;

    public AuthResponse() {
    }

    public AuthResponse(String token, long expiresIn, UserVO user) {
        this.token = token;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserVO getUser() {
        return user;
    }

    public void setUser(UserVO user) {
        this.user = user;
    }
}
