package com.abosultan.darbakteststation;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import android.content.pm.Signature;

public class MainActivity extends Activity {
    private static final int PICK_APK = 1001;
    private TextView fileName;
    private TextView apkInfo;
    private Button installApk;
    private File candidateApk;
    private String lastReport = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        fileName = findViewById(R.id.fileName);
        apkInfo = findViewById(R.id.apkInfo);
        Button select = findViewById(R.id.selectApk);
        installApk = findViewById(R.id.installApk);
        installApk.setOnClickListener(v -> installCandidate());
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
        candidateApk = null;
        installApk.setEnabled(false);
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
            String permissionSummary = permissionSummary(candidate.requestedPermissions);
            String signer = signerSummary(pm, temp);
            Set<String> abis = readAbis(temp);
            String abiText = abis.isEmpty() ? "لا توجد مكتبات Native (مرن معماريًا)" : joinAbis(abis);
            boolean acerAbiOk = abis.isEmpty() || abis.contains("x86") || abis.contains("x86_64");
            boolean t3AbiOk = abis.isEmpty() || abis.contains("armeabi-v7a") || abis.contains("armeabi");
            boolean t3Ready = android7Ok && t3AbiOk;
            String version = candidate.versionName == null ? "—" : candidate.versionName;
            long kb = temp.length() / 1024L;

            candidateApk = temp;
            installApk.setEnabled(true);
            lastReport = "الحزمة: " + candidate.packageName +
                    "\nالإصدار المرشح: " + version + " (" + candidate.versionCode + ")" +
                    "\nالمثبت: " + installedText +
                    "\nالمقارنة: " + comparison +
                    "\nMin SDK: " + (minSdk == 0 ? "غير متاح" : minSdk) +
                    "\nتوافق Android 7.1 / API 25: " + (android7Ok ? "✅" : "❌") +
                    "\nالحجم: " + kb + " KB" +
                    "\nالصلاحيات: " + permissionCount + permissionSummary +
                    "\nالتوقيع: " + signer +
                    "\nABI: " + abiText +
                    "\n\nبوابة Acer: " + (acerAbiOk ? "✅ متوافق معماريًا" : "❌ ABI غير مناسب") +
                    "\nبوابة T3: " + (t3Ready ? "✅ متوافق مبدئيًا" : "❌ يحتاج معالجة") +
                    "\nملاحظة: اعتماد T3 النهائي يتطلب الاختبار على الشاشة الحقيقية";
            apkInfo.setText(lastReport);
            saveReport(candidate.packageName, lastReport);
        } catch (Exception e) {
            apkInfo.setText("❌ فشل الفحص: " + e.getClass().getSimpleName());
        }
    }

    private void saveReport(String packageName, String report) {
        if (!getSharedPreferences("station", MODE_PRIVATE).getBoolean("history", true)) return;
        try {
            File dir = new File(getFilesDir(), "reports");
            if (!dir.exists()) dir.mkdirs();
            String safe = packageName.replaceAll("[^A-Za-z0-9._-]", "_");
            File out = new File(dir, safe + "-latest.txt");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(("Darbak Test Station\n" + new java.util.Date() + "\n\n" + report).getBytes("UTF-8"));
            }
        } catch (Exception ignored) { }
    }

    private void installCandidate() {
        if (candidateApk == null || !candidateApk.exists()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(candidateApk), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            apkInfo.append("\n❌ تعذر فتح مثبت الحزم: " + e.getClass().getSimpleName());
        }
    }

    private String permissionSummary(String[] permissions) {
        if (permissions == null || permissions.length == 0) return " (لا توجد)";
        int dangerousLike = 0;
        String[] keys = {"LOCATION","CAMERA","RECORD_AUDIO","READ_","WRITE_","CALL_PHONE","READ_CONTACTS","BODY_SENSORS"};
        for (String p : permissions) for (String k : keys) if (p.contains(k)) { dangerousLike++; break; }
        return " • حساسة محتملة: " + dangerousLike;
    }

    @SuppressWarnings("deprecation")
    private String signerSummary(PackageManager pm, File apk) {
        try {
            PackageInfo signed = pm.getPackageArchiveInfo(apk.getAbsolutePath(), PackageManager.GET_SIGNATURES);
            if (signed == null || signed.signatures == null || signed.signatures.length == 0) return "غير متاح";
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signed.signatures[0].toByteArray());
            StringBuilder b = new StringBuilder();
            for (int i=0;i<Math.min(8,digest.length);i++) b.append(String.format("%02X", digest[i]));
            return "SHA-256 " + b + "…";
        } catch (Exception e) { return "تعذر القراءة"; }
    }

    private Set<String> readAbis(File apk) throws Exception {
        Set<String> result = new LinkedHashSet<>();
        try (ZipFile zip = new ZipFile(apk)) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (!name.startsWith("lib/") || !name.endsWith(".so")) continue;
                String[] parts = name.split("/");
                if (parts.length >= 3) result.add(parts[1]);
            }
        }
        return result;
    }

    private String joinAbis(Set<String> abis) {
        StringBuilder b = new StringBuilder();
        for (String abi : abis) { if (b.length() > 0) b.append(", "); b.append(abi); }
        return b.toString();
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
