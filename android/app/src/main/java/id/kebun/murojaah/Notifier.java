package id.kebun.murojaah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;

/** Membuat saluran notifikasi dengan suara khas Kebun dan menampilkan notifikasi. */
public class Notifier {
    public static final String CH = "km_remind_v1";

    static Uri sound(Context c) {
        return Uri.parse("android.resource://" + c.getPackageName() + "/" + R.raw.kebun_notif);
    }

    public static void channel(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm.getNotificationChannel(CH) != null) return;
        NotificationChannel ch = new NotificationChannel(CH, "Pengingat Kebun Murojaah", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("Pengingat murojaah, tilawah, dan sunnah dengan suara khas Kebun");
        AudioAttributes aa = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        ch.setSound(sound(c), aa);
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0, 140, 90, 140});
        ch.enableLights(true);
        ch.setLightColor(0xFFFF7AA5);
        nm.createNotificationChannel(ch);
    }

    public static void show(Context c, int id, String title, String body, String go) {
        channel(c);
        Intent open = new Intent(c, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (go != null) open.putExtra("go", go);
        PendingIntent pi = PendingIntent.getActivity(c, id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, CH) : new Notification.Builder(c);
        b.setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFFB4507C)
                .setAutoCancel(true)
                .setContentIntent(pi);
        if (Build.VERSION.SDK_INT < 26) {
            b.setSound(sound(c)).setPriority(Notification.PRIORITY_HIGH).setVibrate(new long[]{0, 140, 90, 140});
        }
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(id, b.build());
    }
}
