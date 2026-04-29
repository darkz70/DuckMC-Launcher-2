package com.tungsten.fcl.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.Logging;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;

public class CustomAuthService {
    private static final String AUTH_SERVER_URL = "https://your-auth-server.com"; // Change this to your server URL
    private static final String PREFS_NAME = "CustomAuthPrefs";
    private static final String TOKEN_KEY = "auth_token";
    private static final String USER_KEY = "auth_user";

    private static CustomAuthService instance;
    private Context context;

    private CustomAuthService(Context context) {
        this.context = context;
    }

    public static synchronized CustomAuthService getInstance(Context context) {
        if (instance == null) {
            instance = new CustomAuthService(context);
        }
        return instance;
    }

    public Task<AuthResponse> login(String username, String password) {
        return new Task<AuthResponse>() {
            @Override
            public void execute() throws Exception {
                try {
                    URL url = new URL(AUTH_SERVER_URL + "/api/auth/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);
                    requestBody.put("password", password);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    String response = readResponse(conn);

                    if (responseCode == 200) {
                        JSONObject jsonResponse = new JSONObject(response);
                        String token = jsonResponse.getString("token");
                        JSONObject user = jsonResponse.getJSONObject("user");

                        // Save token and user info
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit()
                                .putString(TOKEN_KEY, token)
                                .putString(USER_KEY, user.toString())
                                .apply();

                        AuthResponse authResponse = new AuthResponse();
                        authResponse.token = token;
                        authResponse.username = user.getString("username");
                        authResponse.uuid = user.getString("uuid");
                        authResponse.email = user.getString("email");

                        setResult(authResponse);
                    } else {
                        JSONObject errorResponse = new JSONObject(response);
                        throw new Exception(errorResponse.getString("error"));
                    }
                } catch (Exception e) {
                    Logging.LOG.log(Level.SEVERE, "Login failed", e);
                    throw e;
                }
            }
        };
    }

    public Task<AuthResponse> register(String username, String password, String email) {
        return new Task<AuthResponse>() {
            @Override
            public void execute() throws Exception {
                try {
                    URL url = new URL(AUTH_SERVER_URL + "/api/auth/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("username", username);
                    requestBody.put("password", password);
                    requestBody.put("email", email);

                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.toString().getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();
                    String response = readResponse(conn);

                    if (responseCode == 201) {
                        JSONObject jsonResponse = new JSONObject(response);
                        String uuid = jsonResponse.getString("uuid");

                        AuthResponse authResponse = new AuthResponse();
                        authResponse.username = username;
                        authResponse.uuid = uuid;
                        authResponse.email = email;

                        setResult(authResponse);
                    } else {
                        JSONObject errorResponse = new JSONObject(response);
                        throw new Exception(errorResponse.getString("error"));
                    }
                } catch (Exception e) {
                    Logging.LOG.log(Level.SEVERE, "Registration failed", e);
                    throw e;
                }
            }
        };
    }

    public String getToken() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(TOKEN_KEY, null);
    }

    public void logout() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .remove(TOKEN_KEY)
                .remove(USER_KEY)
                .apply();
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }

    public static class AuthResponse {
        public String token;
        public String username;
        public String uuid;
        public String email;
    }
}
