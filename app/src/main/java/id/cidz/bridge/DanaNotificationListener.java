package id.cidz.bridge;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class DanaNotificationListener extends NotificationListenerService {
    private static final String TAG = "DanaListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) return;

        String packageName = sbn.getPackageName();
        if (packageName == null) return;

        // Filter package: DANA Bisnis, DANA reguler, SeaBank, BCA, Shopee, dll
        // id.dana = DANA utama/bisnis
        if (!isTargetPackage(packageName)) {
            return;
        }

        Notification notification = sbn.getNotification();
        if (notification == null) return;

        Bundle extras = notification.extras;
        if (extras == null) return;

        CharSequence titleChar = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence textChar = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigTextChar = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        String title = titleChar != null ? titleChar.toString() : "";
        String text = textChar != null ? textChar.toString() : "";
        if (bigTextChar != null && bigTextChar.length() > text.length()) {
            text = bigTextChar.toString();
        }

        if (title.isEmpty() && text.isEmpty()) return;

        // Skip non-transaksi jika perlu, tapi forwarder PG server sudah pintar memfilter regex nominal
        Log.d(TAG, "Notification captured [" + packageName + "] " + title + " : " + text);

        // Kirim asinkron ke server PG
        HttpSender.sendNotification(getApplicationContext(), packageName, title, text, (success, code, res) -> {
            Log.d(TAG, "Webhook forwarded -> success=" + success + " code=" + code);
        });
    }

    private boolean isTargetPackage(String pkg) {
        return pkg.equals("id.dana")
                || pkg.equals("id.dana.bisnis")
                || pkg.equals("com.shopee.id")
                || pkg.equals("com.bca")
                || pkg.equals("com.btpn.dc")
                || pkg.equals("id.co.seabank.shopee");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "Notification Listener Connected 24/7!");
        // Kirim heartbeat saat listener tersambung
        HttpSender.sendHeartbeat(getApplicationContext(), null);
    }
}
