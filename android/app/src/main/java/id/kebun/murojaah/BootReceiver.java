package id.kebun.murojaah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Memasang ulang alarm setelah HP menyala kembali atau aplikasi diperbarui. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        Scheduler.apply(c, Scheduler.load(c));
    }
}
