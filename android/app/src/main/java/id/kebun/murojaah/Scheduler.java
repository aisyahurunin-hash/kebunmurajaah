package id.kebun.murojaah;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;

/** Menjadwalkan pengingat harian lewat AlarmManager, tetap jalan walau aplikasi ditutup. */
public class Scheduler {
    static final String PREF = "km_rem";
    static final int MAX_N = 6;

    static PendingIntent pi(Context c, int idx, int n, int flags) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("id.kebun.murojaah.REMIND");
        i.putExtra("idx", idx);
        i.putExtra("n", n);
        return PendingIntent.getBroadcast(c, 100 + idx * 10 + n, i, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void save(Context c, String json) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString("json", json).apply();
        apply(c, json);
    }

    public static String load(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("json", null);
    }

    public static void apply(Context c, String json) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        for (int idx = 0; idx < 6; idx++)
            for (int n = 0; n < MAX_N; n++) {
                PendingIntent p = pi(c, idx, n, PendingIntent.FLAG_NO_CREATE);
                if (p != null) { am.cancel(p); p.cancel(); }
            }
        if (json == null) return;
        try {
            JSONObject o = new JSONObject(json);
            int repeat = o.optInt("repeat", 60), max = Math.min(MAX_N, o.optInt("max", 3));
            JSONArray items = o.getJSONArray("items");
            for (int idx = 0; idx < items.length(); idx++) {
                JSONObject it = items.getJSONObject(idx);
                if (!it.optBoolean("on")) continue;
                for (int n = 0; n < max; n++) scheduleNext(c, idx, n, it.optString("t", "16:00"), repeat, false);
            }
        } catch (Exception e) { /* json rusak: abaikan */ }
    }

    /** Jadwalkan alarm ke-n item idx pada kemunculan berikutnya (hari ini jika belum lewat, kalau tidak besok). */
    public static void scheduleNext(Context c, int idx, int n, String hhmm, int repeat, boolean forceTomorrow) {
        try {
            String[] p = hhmm.split(":");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(p[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(p[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.add(Calendar.MINUTE, n * repeat);
            if (forceTomorrow || cal.getTimeInMillis() <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1);
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pend = pi(c, idx, n, PendingIntent.FLAG_UPDATE_CURRENT);
            long t = cal.getTimeInMillis();
            set(c, am, t, pend);
        } catch (Exception e) { /* abaikan */ }
    }

    /** Pasang satu alarm. Pakai alarm tepat bila boleh; kalau tidak, pakai alarm-jam (tetap tepat waktu, kebal Doze). */
    static void set(Context c, AlarmManager am, long t, PendingIntent pend) {
        boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
        if (exact) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pend);
        } else {
            Intent o = new Intent(c, MainActivity.class);
            PendingIntent show = PendingIntent.getActivity(c, 7, o, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            am.setAlarmClock(new AlarmManager.AlarmClockInfo(t, show), pend);
        }
    }

    static void markFire(Context c, String what) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit()
                .putString("last", new java.text.SimpleDateFormat("dd/MM HH:mm:ss", java.util.Locale.US).format(new java.util.Date()) + " " + what).apply();
    }

    /** Tes: alarm sungguhan dalam 60 detik lewat jalur yang sama dengan pengingat. */
    public static void testAlarm(Context c) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.setAction("id.kebun.murojaah.REMIND");
        i.putExtra("test", true);
        PendingIntent p = PendingIntent.getBroadcast(c, 99, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        set(c, am, System.currentTimeMillis() + 60000, p);
    }

    /** Ringkasan status alarm untuk ditampilkan di web (JSON). */
    public static String info(Context c) {
        try {
            AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
            int count = 0;
            for (int idx = 0; idx < 6; idx++)
                for (int n = 0; n < MAX_N; n++) {
                    PendingIntent p = pi(c, idx, n, PendingIntent.FLAG_NO_CREATE);
                    if (p != null) count++;
                }
            boolean exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms();
            boolean batt = true;
            if (Build.VERSION.SDK_INT >= 23) {
                android.os.PowerManager pm = (android.os.PowerManager) c.getSystemService(Context.POWER_SERVICE);
                batt = pm.isIgnoringBatteryOptimizations(c.getPackageName());
            }
            String json = load(c);
            JSONObject o = new JSONObject();
            o.put("n", count);
            o.put("exact", exact);
            o.put("batt", batt);
            o.put("synced", json != null);
            o.put("last", c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString("last", ""));
            o.put("brand", Build.MANUFACTURER);
            return o.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
