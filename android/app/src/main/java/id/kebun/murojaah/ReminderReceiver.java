package id.kebun.murojaah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Random;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        int idx = in.getIntExtra("idx", 0), n = in.getIntExtra("n", 0);
        try {
            String json = Scheduler.load(c);
            if (json == null) return;
            JSONObject o = new JSONObject(json);
            JSONObject it = o.getJSONArray("items").getJSONObject(idx);
            if (!it.optBoolean("on")) return;
            int repeat = o.optInt("repeat", 60);
            String t = it.optString("t", "16:00");
            Calendar now = Calendar.getInstance();
            String today = String.format("%04d-%02d-%02d", now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
            boolean done = it.optBoolean("done") && today.equals(o.optString("date"));
            boolean late = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE) >= 23 * 60 + 30;
            // jadwalkan kemunculan berikutnya (besok) lebih dulu supaya tidak terputus
            Scheduler.scheduleNext(c, idx, n, t, repeat, true);
            if (done || late) return;
            JSONArray msgs = it.optJSONArray("msgs");
            String k = it.optString("k", "murojaah");
            String body = (msgs != null && msgs.length() > 0) ? msgs.getString(new Random().nextInt(msgs.length())) : "Yuk lanjutkan ibadah harianmu di Kebun Murojaah.";
            String name = "murojaah".equals(k) ? "Murojaah" : ("tilawah".equals(k) ? "Tilawah" : "Sunnah");
            Notifier.show(c, 2000 + idx, "Kebun Murojaah · " + name, body, k);
        } catch (Exception e) { /* abaikan */ }
    }
}
