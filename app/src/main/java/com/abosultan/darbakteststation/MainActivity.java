package com.abosultan.darbakteststation;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends Activity {
    private static final int PICK_APK = 1001;
    private TextView fileName;
    private TextView apkInfo;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        fileName = findViewById(R.id.fileName);
        apkInfo = findViewById(R.id.apkInfo);
        Button select = findViewById(R.id.selectApk);
        findViewById(R.id.openSettings).setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        select.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("application/vnd.android.package-archive");
            startActivityForResult(i, PICK_APK);
        });
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_APK || resultCode != RESULT_OK || data == null) return;
        Uri uri = data.getData();
        if (uri != null) inspect(uri);
    }

    private void inspect(Uri uri) {
        fileName.setText(queryName(uri));
        File temp = new File(getCacheDir(), "candidate.apk");
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            if (in == null) throw new IllegalStateException("NoInputStream");
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);

            PackageManager pm = getPackageManager();
            PackageInfo candidate = pm.getPackageArchiveInfo(temp.getAbsolutePath(),
                    PackageManager.GET_PERMISSIONS | PackageManager.GET_CONFIGURATIONS);
            if (candidate == null) {
                apkInfo.setText("❌ تعذر قراءة بيانات APK");
                return;
            }
            if (candidate.applicationInfo != null) {
                candidate.applicationInfo.sourceDir = temp.getAbsolutePath();
                candidate.applicationInfo.publicSourceDir = temp.getAbsolutePath();
            }

            String installedText = "غير مثبت";
            String comparison = "نسخة جديدة";
            try {
                PackageInfo installed = pm.getPackageInfo(candidate.packageName, 0);
                String installedVersion = installed.versionName == null ? "—" : installed.versionName;
                installedText = installedVersion + " (" + installed.versionCode + ")";
                if (candidate.versionCode > installed.versionCode) comparison = "تحديث أحدث ✅";
                else if (candidate.versionCode == installed.versionCode) comparison = "نفس رقم الإصدار ⚠️";
                else comparison = "أقدم من المثبت ⚠️";
            } catch (PackageManager.NameNotFoundException ignored) { }

            int minSdk = candidate.applicationInfo != null && Build.VERSION.SDK_INT >= 24
                    ? candidate.applicationInfo.minSdkVersion : 0;
            boolean android7Ok = minSdk == 0 || minSdk <= 25;
            int permissionCount = candidate.requestedPermissions == null ? 0 : candidate.requestedPermissions.length;
            String version = candidate.versionName == null ? "—" : candidate.versionName;
            long kb = temp.length() / 1024L;

            apkInfo.setText(
                    "الحزمة: " + candidate.packageName +
                    "\nالإصدار المرشح: " + version + " (" + candidate.versionCode + ")" +
                    "\nالمثبت: " + installedText +
                    "\nالمقارنة: " + comparison +
                    "\nMin SDK: " + (minSdk == 0 ? "غير متاح" : minSdk) +
                    "\nتوافق Android 7.1 / API 25: " + (android7Ok ? "✅" : "❌") +
                    "\nالحجم: " + kb + " KB" +
                    "\nالصلاحيات: " + permissionCount +
                    "\n\nبوابة Acer: جاهز لبدء الاختبارات الأساسية" +
                    "\nبوابة T3: لم تُعتمد بعد");
        } catch (Exception e) {
            apkInfo.setText("❌ فشل الفحص: " + e.getClass().getSimpleName());
        }
    }

    private String queryName(Uri uri) {
        Cursor c = getContentResolver().query(uri, null, null, null, null);
        if (c != null) {
            try {
                int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (i >= 0 && c.moveToFirst()) return c.getString(i);
            } finally { c.close(); }
        }
        return "candidate.apk";
    }
}
