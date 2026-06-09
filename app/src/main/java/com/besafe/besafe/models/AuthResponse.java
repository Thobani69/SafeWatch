package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class AuthResponse {

    @SerializedName("access_token")
    private String accessToken;


    @SerializedName("refresh_token")
    private String refreshToken;

    public String getAccessToken()  { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
}