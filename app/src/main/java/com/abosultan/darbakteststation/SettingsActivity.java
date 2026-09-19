package com.abosultan.darbakteststation;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;
public class SettingsActivity extends Activity {
 @Override public void onCreate(Bundle b){ super.onCreate(b); setContentView(R.layout.activity_settings);
  android.content.SharedPreferences p=getSharedPreferences("station",MODE_PRIVATE);
  bind(R.id.strictT3,"strict_t3",true,p); bind(R.id.showTechnical,"technical",true,p); bind(R.id.keepHistory,"history",true,p);
 }
 private void bind(int id,String key,boolean def,android.content.SharedPreferences p){
  Switch s=findViewById(id); s.setChecked(p.getBoolean(key,def)); s.setOnCheckedChangeListener((v,c)->p.edit().putBoolean(key,c).apply());
 }
}