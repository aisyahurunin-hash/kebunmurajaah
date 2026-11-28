package id.kebun.murojaah;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.app.NotificationManager;
import android.view.Gravity;
import android.os.Handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String SITE = "https://aisyahurunin-hash.github.io/kebunmurajaah/";
    private static final String HOST = "aisyahurunin-hash.github.io";
    private static final int REQ_FILE = 11;
    private static final int REQ_LOC = 12;

    private static final int REQ_NOTIF = 13;
    private static final int REQ_MIC = 14;
    private static final int REQ_MIC2 = 15;
    private android.webkit.PermissionRequest micReq;
    private WebView web;
    private android.speech.SpeechRecognizer sr;

    private void srText(String type, Bundle b) {
        java.util.ArrayList<String> l = b == null ? null : b.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        srJs(type, (l != null && l.size() > 0) ? l.get(0) : "");
    }

    private void srJs(final String type, final String data) {
        final String js = "window.__krNative&&window.__krNative(" + org.json.JSONObject.quote(type) + "," + org.json.JSONObject.quote(data) + ")";
        runOnUiThread(new Runnable() {
            public void run() { try { web.evaluateJavascript(js, null); } catch (Exception e) { } }
        });
    }


    private boolean keepAlive = false;
    private LinearLayout splash;
    private ValueCallback<Uri[]> fileCallback;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#FFF5F9"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        FrameLayout root = new FrameLayout(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));
        splash = new LinearLayout(this);
        splash.setOrientation(LinearLayout.VERTICAL);
        splash.setGravity(Gravity.CENTER);
        splash.setBackgroundColor(Color.parseColor("#FFEAF2"));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.mipmap.ic_launcher);
        splash.addView(logo, new LinearLayout.LayoutParams(260, 260));
        TextView tv = new TextView(this);
        tv.setText("Kebun Murojaah");
        tv.setTextColor(Color.parseColor("#B0457A"));
        tv.setTextSize(22);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 28, 0, 0);
        splash.addView(tv);
        root.addView(splash, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        new Handler().postDelayed(new Runnable() { public void run() { hideSplash(); } }, 7000);
        Notifier.channel(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
        }

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(false);
        s.setGeolocationEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(s.getUserAgentString() + " KebunApp/8.3");

        web.addJavascriptInterface(new Bridge(), "KebunApp");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost();
                String scheme = u.getScheme();
                if (HOST.equals(host) || "about".equals(scheme) || "data".equals(scheme) || "blob".equals(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Tidak bisa membuka tautan", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                new Handler().postDelayed(new Runnable() { public void run() { hideSplash(); } }, 450);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOffline();
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                boolean wantsMic = false;
                for (String r : request.getResources()) {
                    if (android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(r)) wantsMic = true;
                }
                if (!wantsMic) { request.deny(); return; }
                if (Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    request.grant(new String[]{android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE});
                } else {
                    micReq = request;
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
                }
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false);
                } else {
                    geoOrigin = origin;
                    geoCallback = callback;
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOC);
                }
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(SITE + goHash(getIntent()));
        }
    }

    private String goHash(Intent i) {
        String g = i == null ? null : i.getStringExtra("go");
        return (g != null && g.matches("[a-z]+")) ? "#" + g : "";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String h = goHash(intent);
        if (h.length() > 0) web.loadUrl(SITE + h);
    }

    private void hideSplash() {
        if (splash == null) return;
        final View s = splash;
        splash = null;
        s.animate().alpha(0f).setDuration(350).withEndAction(new Runnable() {
            public void run() { ((android.view.ViewGroup) s.getParent()).removeView(s); }
        }).start();
    }

    private void showOffline() {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='margin:0;font-family:sans-serif;background:#fff5f9;color:#85556a;display:flex;"
                + "align-items:center;justify-content:center;min-height:100vh;text-align:center'>"
                + "<div style='padding:28px'><div style='font-size:64px'>&#127800;</div>"
                + "<h2 style='margin:8px 0'>Belum ada internet</h2>"
                + "<p>Kebun Murojaah perlu terhubung sekali supaya bisa dibuka. Setelah itu, bisa dibuka juga saat sinyal jelek.</p>"
                + "<a href='" + SITE + "' style='display:inline-block;margin-top:12px;padding:12px 26px;"
                + "border-radius:999px;background:#b4507c;color:#fff;font-weight:bold;text-decoration:none'>Coba lagi</a>"
                + "</div></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                Uri[] result = null;
                if (resultCode == RESULT_OK) {
                    result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
                }
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQ_MIC2) {
            boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!ok && Build.VERSION.SDK_INT >= 23 && !shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)) {
                /* sudah ditolak permanen: Android tidak akan menampilkan jendela lagi, jadi buka pengaturan aplikasi */
                android.widget.Toast.makeText(this, "Aktifkan izin Mikrofon untuk Kebun Murojaah", android.widget.Toast.LENGTH_LONG).show();
                try {
                    startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) { /* abaikan */ }
            }
            return;
        }
        if (requestCode == REQ_MIC && micReq != null) {
            boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (ok) micReq.grant(new String[]{android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE});
            else micReq.deny();
            micReq = null;
            return;
        }
        if (requestCode == REQ_LOC && geoCallback != null) {
            boolean ok = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            geoCallback.invoke(geoOrigin, ok, false);
            geoCallback = null;
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!keepAlive) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    /** Jembatan JavaScript: dipakai web untuk menyimpan file cadangan ke folder Download. */
    private class Bridge {
        @JavascriptInterface
        public void saveFile(String name, String mime, String base64) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.MediaColumns.DISPLAY_NAME, safe);
                    v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                    v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(bytes);
                    os.close();
                } else {
                    File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    dir.mkdirs();
                    FileOutputStream os = new FileOutputStream(new File(dir, safe));
                    os.write(bytes);
                    os.close();
                }
                toast("Tersimpan di folder Download: " + safe);
            } catch (Exception e) {
                toast("Gagal menyimpan file");
            }
        }

        @JavascriptInterface
        public void audioState(final boolean playing, final String title) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        keepAlive = playing;
                        Intent si = new Intent(MainActivity.this, AudioKeepService.class);
                        if (playing) {
                            si.putExtra("t", title == null ? "Murottal diputar" : title);
                            if (Build.VERSION.SDK_INT >= 26) startForegroundService(si); else startService(si);
                        } else {
                            stopService(si);
                            web.onPause();
                        }
                    } catch (Exception e) { /* abaikan: audio tetap jalan di latar bila bisa */ }
                }
            });
        }

        @JavascriptInterface
        public String version() {
            return "8.3";
        }

        @JavascriptInterface
        public void setReminders(String json) {
            Scheduler.save(MainActivity.this, json);
        }

        @JavascriptInterface
        public void notifyNow(String title, String body) {
            Notifier.show(MainActivity.this, 1999, title, body, null);
        }

        /* ===== pengenalan suara bawaan Android (untuk Koreksi Murojaah) ===== */
        @JavascriptInterface
        public boolean srAvailable() {
            try { return android.speech.SpeechRecognizer.isRecognitionAvailable(MainActivity.this); } catch (Exception e) { return false; }
        }

        @JavascriptInterface
        public void srStart(final String lang) {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC2);
                        srJs("error", "not-allowed");
                        return;
                    }
                    try {
                        if (sr != null) { try { sr.destroy(); } catch (Exception e) { } sr = null; }
                        sr = android.speech.SpeechRecognizer.createSpeechRecognizer(MainActivity.this);
                        sr.setRecognitionListener(new android.speech.RecognitionListener() {
                            public void onReadyForSpeech(Bundle b) { srJs("ready", ""); }
                            public void onBeginningOfSpeech() { }
                            public void onRmsChanged(float v) { }
                            public void onBufferReceived(byte[] b) { }
                            public void onEndOfSpeech() { }
                            public void onEvent(int i, Bundle b) { }
                            public void onError(int code) {
                                String e = "error";
                                if (code == android.speech.SpeechRecognizer.ERROR_NO_MATCH || code == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT) e = "no-speech";
                                else if (code == android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) e = "not-allowed";
                                else if (code == android.speech.SpeechRecognizer.ERROR_NETWORK || code == android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT) e = "network";
                                else if (code == android.speech.SpeechRecognizer.ERROR_AUDIO) e = "audio-capture";
                                srJs("error", e);
                            }
                            public void onPartialResults(Bundle b) { srText("partial", b); }
                            public void onResults(Bundle b) { srText("final", b); }
                        });
                        Intent it = new Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, lang);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 1);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
                        it.putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500);
                        sr.startListening(it);
                    } catch (Exception e) {
                        srJs("error", "service-not-allowed");
                    }
                }
            });
        }

        @JavascriptInterface
        public void srStop() {
            runOnUiThread(new Runnable() {
                public void run() { try { if (sr != null) sr.stopListening(); } catch (Exception e) { } }
            });
        }

        @JavascriptInterface
        public void srCancel() {
            runOnUiThread(new Runnable() {
                public void run() { try { if (sr != null) { sr.cancel(); sr.destroy(); sr = null; } } catch (Exception e) { } }
            });
        }

        @JavascriptInterface
        public boolean micOK() {
            return Build.VERSION.SDK_INT < 23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        }

        @JavascriptInterface
        public void askMic() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC2);
                    }
                }
            });
        }

        @JavascriptInterface
        public void openSettings() {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) { /* abaikan */ }
                }
            });
        }

        @JavascriptInterface
        public String alarmInfo() {
            return Scheduler.info(MainActivity.this);
        }

        @JavascriptInterface
        public void testAlarm() {
            Scheduler.testAlarm(MainActivity.this);
        }

        @JavascriptInterface
        public void askExact() {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        if (Build.VERSION.SDK_INT >= 31) {
                            startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                        }
                    } catch (Exception e) { /* abaikan */ }
                }
            });
        }

        @JavascriptInterface
        public void askBattery() {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        startActivity(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        try { startActivity(new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); } catch (Exception e2) { /* abaikan */ }
                    }
                }
            });
        }

        @JavascriptInterface
        public boolean notifOK() {
            return ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).areNotificationsEnabled();
        }

        @JavascriptInterface
        public void askNotif() {
            runOnUiThread(new Runnable() {
                public void run() {
                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIF);
                    }
                }
            });
        }
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
