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

public class MainActivity extends Activity {
    private EditText etUrl, etKey;
    private TextView tvStatus, tvPermissionStatus;
    private Button btnSave, btnTest, btnPermission, btnBattery;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("cidz_cfg", Context.MODE_PRIVATE);

        etUrl = findViewById(R.id.etUrl);
        etKey = findViewById(R.id.etKey);
        tvStatus = findViewById(R.id.tvStatus);
        tvPermissionStatus = findViewById(R.id.tvPermissionStatus);
        btnSave = findViewById(R.id.btnSave);
        btnTest = findViewById(R.id.btnTest);
        btnPermission = findViewById(R.id.btnPermission);
        btnBattery = findViewById(R.id.btnBattery);

        // Load saved
        etUrl.setText(sp.getString("url", "https://pg.cidz.ai.id/webhook/dana"));
        etKey.setText(sp.getString("key", ""));

        btnSave.setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            String key = etKey.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "URL Webhook tidak boleh kosong", Toast.LENGTH_SHORT).show();
                return;
            }
            sp.edit().putString("url", url).putString("key", key).apply();
            Toast.makeText(this, "Konfigurasi tersimpan!", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        btnTest.setOnClickListener(v -> {
            tvStatus.setText("Mengirim test webhook simulasi...");
            HttpSender.sendNotification(this, "id.dana", "DANA Bisnis", "Pembayaran QRIS Rp 1.000 dari Pelanggan", (success, code, res) -> {
                runOnUiThread(() -> {
                    if (success) {
                        tvStatus.setText("✅ Berhasil terkirim! (HTTP " + code + ")");
                    } else {
                        tvStatus.setText("❌ Gagal: " + res + " (HTTP " + code + ")");
                    }
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
                    Toast.makeText(this, "Optimasi baterai sudah dinonaktifkan (Aman)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    private void updateUI() {
        boolean hasPermission = isNotificationServiceEnabled();
        if (hasPermission) {
            tvPermissionStatus.setText("Status Izin Notifikasi: ✅ AKTIF 24/7");
            tvPermissionStatus.setTextColor(0xFF00FF88); // Neon green
            btnPermission.setVisibility(View.GONE);
        } else {
            tvPermissionStatus.setText("Status Izin Notifikasi: ❌ BELUM DIIZINKAN");
            tvPermissionStatus.setTextColor(0xFFFF4444); // Red
            btnPermission.setVisibility(View.VISIBLE);
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
