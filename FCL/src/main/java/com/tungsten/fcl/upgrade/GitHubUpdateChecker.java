package com.tungsten.fcl.upgrade;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tungsten.fcl.R;
import com.tungsten.fclcore.util.io.HttpRequest;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class GitHubUpdateChecker {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/darkz70/DuckMC-Launcher-2/releases/latest";
    private static final String CURRENT_VERSION = "1.3.0.2";
    
    private static GitHubUpdateChecker instance;
    
    public static GitHubUpdateChecker getInstance() {
        if (instance == null) {
            instance = new GitHubUpdateChecker();
        }
        return instance;
    }
    
    public CompletableFuture<Void> checkUpdate(Context context) {
        return CompletableFuture.runAsync(() -> {
            try {
                String response = HttpRequest.GET(GITHUB_API_URL).getResponseString();
                JsonObject json = JsonParser.parseString(response).getAsJsonObject();
                
                String latestVersion = json.get("tag_name").getAsString().replaceFirst("^v", "");
                String downloadUrl = null;
                
                // Find APK download URL
                JsonArray assets = json.getAsJsonArray("assets");
                for (int i = 0; i < assets.size(); i++) {
                    JsonObject asset = assets.get(i).getAsJsonObject();
                    String name = asset.get("name").getAsString();
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.get("browser_download_url").getAsString();
                        break;
                    }
                }
                
                if (isNewerVersion(latestVersion, CURRENT_VERSION)) {
                    showUpdateDialog(context, latestVersion, downloadUrl);
                } else {
                    showToast(context, "You are using the latest version");
                }
            } catch (IOException e) {
                showToast(context, "Failed to check for updates");
                e.printStackTrace();
            }
        });
    }
    
    private boolean isNewerVersion(String latest, String current) {
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        
        for (int i = 0; i < Math.min(latestParts.length, currentParts.length); i++) {
            try {
                int latestNum = Integer.parseInt(latestParts[i]);
                int currentNum = Integer.parseInt(currentParts[i]);
                
                if (latestNum > currentNum) return true;
                if (latestNum < currentNum) return false;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        
        return latestParts.length > currentParts.length;
    }
    
    private void showUpdateDialog(Context context, String version, String downloadUrl) {
        // This will be called from UI thread
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle("Update Available")
                .setMessage("New version " + version + " is available. Download now?")
                .setPositiveButton("Download", (dialog, which) -> {
                    if (downloadUrl != null) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
                        context.startActivity(intent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showToast(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
