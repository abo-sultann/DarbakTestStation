package com.abosultan.darbakteststation;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.database.Cursor;
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
        if (uri == null) return;
        inspect(uri);
    }

    private void inspect(Uri uri) {
        String displayName = queryName(uri);
        fileName.setText(displayName);
        File temp = new File(getCacheDir(), "candidate.apk");
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(temp)) {
            byte[] buffer = new byte[32768];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);

            PackageManager pm = getPackageManager();
            PackageInfo p = pm.getPackageArchiveInfo(temp.getAbsolutePath(), PackageManager.GET_PERMISSIONS);
            if (p == null) {
                apkInfo.setText("تعذر قراءة بيانات APK");
                return;
            }
            String version = p.versionName == null ? "—" : p.versionName;
            long kb = temp.length() / 1024L;
            int permissionCount = p.requestedPermissions == null ? 0 : p.requestedPermissions.length;
            apkInfo.setText("الحزمة: " + p.packageName +
                    "\nالإصدار: " + version +
                    "\nالحجم: " + kb + " KB" +
                    "\nالصلاحيات المطلوبة: " + permissionCount +
                    "\n\nحالة V1: تم فحص بيانات APK الأساسية.");
        } catch (Exception e) {
            apkInfo.setText("فشل الفحص: " + e.getClass().getSimpleName());
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
