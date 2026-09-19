package com.abosultan.darbakteststation;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import java.io.*;

public class ReportsActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state); setContentView(R.layout.activity_reports);
  TextView view=findViewById(R.id.reportsText);
  File dir=new File(getFilesDir(),"reports"); File[] files=dir.listFiles();
  if(files==null||files.length==0) return;
  java.util.Arrays.sort(files,(a,b)->Long.compare(b.lastModified(),a.lastModified()));
  StringBuilder out=new StringBuilder();
  for(File f:files){ if(!f.isFile()) continue; out.append("━━ ").append(f.getName()).append(" ━━\n"); try{
    BufferedReader r=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8")); String line;
    while((line=r.readLine())!=null) out.append(line).append('\n'); r.close();
  }catch(Exception e){out.append("تعذر قراءة التقرير\n");} out.append('\n'); }
  view.setText(out.toString());
 }
}