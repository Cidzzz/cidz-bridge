package id.cidz.bridge;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class HttpSender {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onResult(boolean success, int code, String response);
    }

    public static void sendNotification(Context ctx, String pkg, String title, String text, Callback cb) {
        SharedPreferences sp = ctx.getSharedPreferences("cidz_cfg", Context.MODE_PRIVATE);
        String targetUrl = sp.getString("url", "https://pg.cidz.ai.id/webhook/dana");
        String key = sp.getString("key", "");

        executor.execute(() -> {
            try {
                String json = "{"
                        + "\"package\":\"" + escapeJson(pkg) + "\","
                        + "\"title\":\"" + escapeJson(title) + "\","
                        + "\"text\":\"" + escapeJson(text) + "\","
                        + "\"timestamp\":" + System.currentTimeMillis()
                        + "}";

                HttpURLConnection conn = (HttpURLConnection) new URL(targetUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("User-Agent", "CidzBridge/1.0 (Android)");
                if (!key.isEmpty()) {
                    conn.setRequestProperty("X-Forwarder-Key", key);
                }
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setDoOutput(true);

                byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                    os.flush();
                }

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);

                // Read response body
                String respBody = readStream(ok ? conn.getInputStream() : conn.getErrorStream());
                conn.disconnect();

                // Save to local logs (in memory SharedPreferences)
                addLog(ctx, pkg, title, text, code, ok, respBody);

                if (cb != null) cb.onResult(ok, code, respBody.isEmpty() ? ("HTTP " + code) : respBody);
            } catch (Exception e) {
                addLog(ctx, pkg, title, text, -1, false, e.getMessage());
                if (cb != null) cb.onResult(false, -1, e.getMessage());
            }
        });
    }

    public static void sendHeartbeat(Context ctx, Callback cb) {
        SharedPreferences sp = ctx.getSharedPreferences("cidz_cfg", Context.MODE_PRIVATE);
        String baseUrl = sp.getString("url", "https://pg.cidz.ai.id/webhook/dana");
        String key = sp.getString("key", "");

        String hbUrl = baseUrl.contains("/webhook/") 
                ? baseUrl.substring(0, baseUrl.lastIndexOf("/webhook/")) + "/webhook/heartbeat"
                : baseUrl + "/webhook/heartbeat";

        executor.execute(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(hbUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "CidzBridge/1.0 (Android)");
                if (!key.isEmpty()) {
                    conn.setRequestProperty("X-Forwarder-Key", key);
                }
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write("{}".getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                boolean ok = (code >= 200 && code < 300);
                conn.disconnect();
                if (cb != null) cb.onResult(ok, code, "Heartbeat " + code);
            } catch (Exception e) {
                if (cb != null) cb.onResult(false, -1, e.getMessage());
            }
        });
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }

    private static synchronized void addLog(Context ctx, String pkg, String title, String text, int code, boolean ok, String resp) {
        try {
            SharedPreferences sp = ctx.getSharedPreferences("cidz_logs", Context.MODE_PRIVATE);
            String rawLogs = sp.getString("history", "[]");
            JSONArray arr = new JSONArray(rawLogs);

            JSONObject item = new JSONObject();
            String timeStr = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
            item.put("time", timeStr);
            item.put("pkg", pkg);
            item.put("title", title);
            item.put("text", text);
            item.put("code", code);
            item.put("ok", ok);
            item.put("resp", resp);

            // Prepend new item
            JSONArray newArr = new JSONArray();
            newArr.put(item);
            for (int i = 0; i < Math.min(arr.length(), 29); i++) { // simpan max 30 log
                newArr.put(arr.get(i));
            }
            sp.edit().putString("history", newArr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '"') sb.append("\\\"");
            else if (ch == '\\') sb.append("\\\\");
            else if (ch == '\b') sb.append("\\b");
            else if (ch == '\f') sb.append("\\f");
            else if (ch == '\n') sb.append("\\n");
            else if (ch == '\r') sb.append("\\r");
            else if (ch == '\t') sb.append("\\t");
            else if (ch < ' ') {
                String t = "000" + Integer.toHexString(ch);
                sb.append("\\u").append(t.substring(t.length() - 4));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
