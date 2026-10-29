package id.cidz.bridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i("BootReceiver", "Device rebooted. Cidz Bridge waking up listener.");
            // Kirim heartbeat tanda HP baru nyala
            HttpSender.sendHeartbeat(context.getApplicationContext(), null);
        }
    }
}
