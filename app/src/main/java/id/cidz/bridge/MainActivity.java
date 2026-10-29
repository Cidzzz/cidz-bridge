package id.cidz.bridge;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private EditText etUrl, etKey, etTestAmount;
    private TextView tvBadgeStatus, tvPermissionDesc, tvTestStatus, tvLogsContent, tvClearLogs;
    private Button btnSave, btnTest, btnPermission, btnBattery;
    private SharedPreferences spCfg, spLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spCfg = getSharedPreferences("cidz_cfg", Context.MODE_PRIVATE);
        spLogs = getSharedPreferences("cidz_logs", Context.MODE_PRIVATE);

        tvBadgeStatus = findViewById(R.id.tvBadgeStatus);
        tvPermissionDesc = findViewById(R.id.tvPermissionDesc);
        btnPermission = findViewById(R.id.btnPermission);
        btnBattery = findViewById(R.id.btnBattery);

        etTestAmount = findViewById(R.id.etTestAmount);
        btnTest = findViewById(R.id.btnTest);
        tvTestStatus = findViewById(R.id.tvTestStatus);

        etUrl = findViewById(R.id.etUrl);
        etKey = findViewById(R.id.etKey);
        btnSave = findViewById(R.id.btnSave);

        tvLogsContent = findViewById(R.id.tvLogsContent);
        tvClearLogs = findViewById(R.id.tvClearLogs);

        // Load config
        etUrl.setText(spCfg.getString("url", "https://pg.cidz.ai.id/webhook/dana"));
        etKey.setText(spCfg.getString("key", ""));

        btnSave.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String key = etKey.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "URL tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            spCfg.edit().putString("url", url).putString("key", key).apply();
            Toast.makeText(this, "Konfigurasi disimpan!", Toast.LENGTH_SHORT).show();
            renderLogs();
        });

        btnTest.setOnClickListener(v -> {
            String amountStr = etTestAmount.getText().toString().trim();
            if (amountStr.isEmpty()) amountStr = "1221";

            tvTestStatus.setText("Mengirim simulasi Rp " + amountStr + "...");
            String dummyTitle = "DANA Bisnis";
            String dummyText = "Pembayaran QRIS Rp " + amountStr + " dari Pelanggan Sukses";

            HttpSender.sendNotification(this, "id.dana", dummyTitle, dummyText, (success, code, res) -> {
                runOnUiThread(() -> {
                    if (success) {
                        tvTestStatus.setText("✅ Berhasil dikirim ke PG! (" + code + ")");
                    } else {
                        tvTestStatus.setText("❌ Gagal: " + res + " (" + code + ")");
                    }
                    renderLogs();
                });
            });
        });

        btnPermission.setOnClickListener(v -> {
            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
        });

        btnBattery.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Intent intent = new Intent();
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + packageName));
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Bypass optimasi baterai sudah aktif!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvClearLogs.setOnClickListener(v -> {
            spLogs.edit().remove("history").apply();
            renderLogs();
            Toast.makeText(this, "Riwayat dibersihkan", Toast.LENGTH_SHORT).show();
        });

        updateUI();
        renderLogs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        renderLogs();
    }

    private void updateUI() {
        boolean hasPermission = isNotificationServiceEnabled();
        if (hasPermission) {
            tvBadgeStatus.setText("ACTIVE 24/7");
            tvBadgeStatus.setBackgroundColor(0xFF238636); // Green
            tvPermissionDesc.setText("Status: Listener aktif & siap tangkap notifikasi DANA.");
            btnPermission.setVisibility(View.GONE);
        } else {
            tvBadgeStatus.setText("DISABLED");
            tvBadgeStatus.setBackgroundColor(0xFFDA3633); // Red
            tvPermissionDesc.setText("Status: Akses notifikasi belum diizinkan oleh sistem Android.");
            btnPermission.setVisibility(View.VISIBLE);
        }
    }

    private void renderLogs() {
        try {
            String rawLogs = spLogs.getString("history", "[]");
            JSONArray arr = new JSONArray(rawLogs);
            if (arr.length() == 0) {
                tvLogsContent.setText("Belum ada riwayat notifikasi.");
                return;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String time = obj.optString("time", "");
                String pkg = obj.optString("pkg", "");
                String text = obj.optString("text", "");
                int code = obj.optInt("code", 0);
                boolean ok = obj.optBoolean("ok", false);

                sb.append("[").append(time).append("] ")
                  .append(ok ? "✅ " : "❌ ")
                  .append("HTTP ").append(code).append("\n")
                  .append("App: ").append(pkg).append("\n")
                  .append("Data: ").append(text).append("\n");

                if (i < arr.length() - 1) {
                    sb.append("────────────────────────────\n");
                }
            }
            tvLogsContent.setText(sb.toString().trim());
        } catch (Exception e) {
            tvLogsContent.setText("Error membaca riwayat.");
        }
    }

    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
