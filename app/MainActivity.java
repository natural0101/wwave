package local.tclbrightness;
import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
 int dayLevel,nightLevel;String nightStart,nightEnd;TextView dayValue,nightValue,status,detail;Button startTime,endTime;Switch enabled;boolean updating=false;Handler handler=new Handler();
 SharedPreferences prefs(){return getSharedPreferences("schedule",0);}
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
 TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);return t;}
 GradientDrawable surface(int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(9));return d;}
 StateListDrawable buttonBackground(){StateListDrawable d=new StateListDrawable();d.addState(new int[]{android.R.attr.state_focused},surface(0xff626dad));d.addState(new int[]{android.R.attr.state_pressed},surface(0xff505d99));d.addState(new int[]{},surface(0xffffffff));return d;}
 Button button(String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{android.R.attr.state_pressed},new int[]{}},new int[]{Color.WHITE,Color.WHITE,0xff1c4c77}));b.setTextSize(15);b.setAllCaps(false);b.setBackground(buttonBackground());b.setMinHeight(dp(42));b.setPadding(dp(12),dp(3),dp(12),dp(3));b.setOnClickListener(v->{try{action.run();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}});return b;}
 void addButton(LinearLayout parent,Button b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(43));p.topMargin=dp(9);parent.addView(b,p);}
 LinearLayout card(String title){LinearLayout c=column();c.setPadding(dp(20),dp(16),dp(20),dp(16));c.setBackground(surface(0xfff0efff));TextView t=text(title,20,0xff26334b);t.setTypeface(null,Typeface.BOLD);c.addView(t);return c;}
 @Override public void onCreate(Bundle state){super.onCreate(state);SharedPreferences p=prefs();dayLevel=p.getInt("day",30);nightLevel=p.getInt("night",10);nightStart=p.getString("start","21:00");nightEnd=p.getString("end","09:00");
  LinearLayout root=column();root.setPadding(dp(40),dp(24),dp(40),dp(20));root.setBackgroundColor(0xfffafaff);setContentView(root);
  LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);ImageView mark=new ImageView(this);mark.setImageResource(getResources().getIdentifier("ww_mark","drawable",getPackageName()));LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(100),dp(48));mp.rightMargin=dp(12);header.addView(mark,mp);header.addView(new View(this),new LinearLayout.LayoutParams(0,dp(48),1));enabled=new Switch(this);enabled.setText("Расписание  ");enabled.setTextSize(18);enabled.setTextColor(0xff26334b);enabled.setChecked(p.getBoolean("enabled",false));enabled.setPadding(dp(18),0,0,0);enabled.setFocusable(true);header.addView(enabled,new LinearLayout.LayoutParams(dp(265),dp(48)));root.addView(header);
  LinearLayout intro=new LinearLayout(this);intro.setGravity(Gravity.CENTER_VERTICAL);TextView explanation=text("Яркость телевизора · день и ночь по времени",16,0xff4e6b81);intro.addView(explanation,new LinearLayout.LayoutParams(0,dp(38),1));intro.addView(button("Подключение",()->connect()),new LinearLayout.LayoutParams(dp(160),dp(36)));root.addView(intro,new LinearLayout.LayoutParams(-1,dp(42)));
  LinearLayout cards=new LinearLayout(this);LinearLayout levels=card("Уровни яркости"),times=card("Время переключения");times.setBackground(surface(0xffedf8f7));LinearLayout.LayoutParams left=new LinearLayout.LayoutParams(0,dp(300),1);left.rightMargin=dp(16);cards.addView(levels,left);cards.addView(times,new LinearLayout.LayoutParams(0,dp(300),1));root.addView(cards);
  dayValue=levelRow(levels,"День",true);nightValue=levelRow(levels,"Ночь",false);
  addButton(levels,button("Проверить ночную яркость",()->{save();ScheduleService.start(this,"night");}));addButton(levels,button("Вернуть дневную яркость",()->{save();ScheduleService.start(this,"day");}));
  startTime=button("Ночной режим с "+nightStart,()->pickTime(true));endTime=button("Дневной режим с "+nightEnd,()->pickTime(false));addButton(times,startTime);addButton(times,endTime);addButton(times,button("Сохранить настройки",()->{save();if(prefs().getBoolean("enabled",false))ScheduleService.start(this,"start");Toast.makeText(this,"Настройки сохранены",Toast.LENGTH_SHORT).show();}));addButton(times,button("Отключить и вернуть дневную яркость",()->{save();ScheduleService.start(this,"stop");updating=true;enabled.setChecked(false);updating=false;}));
  status=text("",18,0xff1b7955);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(36));sp.topMargin=dp(12);root.addView(status,sp);detail=text("",14,0xff4e6b81);root.addView(detail);
  TextView note=text("При изменении меню TCL появляется на 10–20 секунд. Для работы нужен включённый ADB. Выключенный телевизор приложение не включает.",12,0xff586f7c);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(6);root.addView(note,np);
  enabled.setOnCheckedChangeListener((b,on)->{if(updating)return;save();ScheduleService.start(this,on?"start":"stop");});refresh();enabled.requestFocus();if(p.getBoolean("enabled",false))ScheduleService.start(this,"tick");
 }
 void connect(){
  if(prefs().getBoolean("working",false)){Toast.makeText(this,"Дождитесь завершения изменения яркости",Toast.LENGTH_LONG).show();return;}
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Подключение WWave").setMessage("Включите сетевую отладку ADB на телевизоре (порт 5555). При первом подключении выберите «Всегда разрешать» и «Разрешить» в запросе отладки. Проверяю подключение…").setPositiveButton("Закрыть",null).create();dialog.show();
  new Thread(()->{String result;try(LocalAdb adb=new LocalAdb(this)){String id=adb.shell("id");if(!id.contains("uid=2000(shell)"))throw new IllegalStateException("ADB не предоставил доступ shell");result="Подключение проверено. Можно включить расписание. Авторизация сохранена на телевизоре.";}catch(Exception e){result="Подключиться не удалось: "+e.getMessage()+". Проверьте сетевую отладку (порт 5555), разрешите запрос и повторите проверку.";}final String message=result;handler.post(()->{if(!isFinishing()&&dialog.isShowing())dialog.setMessage(message);});}).start();
 }
 TextView levelRow(LinearLayout parent,String label,boolean day){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);TextView t=text(label,17,0xff3e5872);t.setGravity(Gravity.CENTER_VERTICAL);row.addView(t,new LinearLayout.LayoutParams(0,dp(47),1));TextView value=text(""+(day?dayLevel:nightLevel)+" / 100",18,0xff205fa8);value.setGravity(Gravity.CENTER);Button minus=button("−5",()->changeLevel(day,-5));Button plus=button("+5",()->changeLevel(day,5));row.addView(minus,new LinearLayout.LayoutParams(dp(54),dp(38)));row.addView(value,new LinearLayout.LayoutParams(dp(88),dp(47)));row.addView(plus,new LinearLayout.LayoutParams(dp(54),dp(38)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(51));p.topMargin=dp(9);parent.addView(row,p);return value;}
 void changeLevel(boolean day,int delta){if(day){dayLevel=Math.max(1,Math.min(100,dayLevel+delta));dayValue.setText(dayLevel+" / 100");}else{nightLevel=Math.max(1,Math.min(100,nightLevel+delta));nightValue.setText(nightLevel+" / 100");}}
 void pickTime(boolean start){String[] parts=(start?nightStart:nightEnd).split(":");TimePickerDialog dialog=new TimePickerDialog(this,android.R.style.Theme_Holo_Light_Dialog,(view,h,m)->{String value=String.format(Locale.ROOT,"%02d:%02d",h,m);if(start){nightStart=value;startTime.setText("Ночной режим с "+value);}else{nightEnd=value;endTime.setText("Дневной режим с "+value);}},Integer.parseInt(parts[0]),Integer.parseInt(parts[1]),true);dialog.setTitle(start?"Начало ночного режима":"Возврат дневной яркости");dialog.show();}
 void save(){if(nightStart.equals(nightEnd))throw new IllegalArgumentException("Время начала и окончания должно отличаться");prefs().edit().putInt("day",dayLevel).putInt("night",nightLevel).putString("start",nightStart).putString("end",nightEnd).apply();}
 Runnable poll=new Runnable(){public void run(){refresh();handler.postDelayed(this,1000);}};
 void setTextIfChanged(TextView view,String value){if(!value.equals(view.getText().toString()))view.setText(value);}
 void refresh(){if(status==null)return;SharedPreferences p=prefs();updating=true;enabled.setChecked(p.getBoolean("enabled",false));updating=false;String state=p.getBoolean("enabled",false)?"Расписание включено":"Расписание выключено";if(p.getBoolean("working",false))state+=" · меняю яркость…";setTextIfChanged(status,state);String error=p.getString("lastError","");int color=error.isEmpty()?0xff1b7955:0xffa73529;if(status.getCurrentTextColor()!=color)status.setTextColor(color);if(!error.isEmpty()){setTextIfChanged(detail,"Ошибка: "+p.getString("result",error));}else{long at=p.getLong("lastSuccess",0);setTextIfChanged(detail,at==0?"Яркость ещё не проверена": "Последняя проверенная яркость: "+p.getInt("lastActual",0)+" / 100 · "+new SimpleDateFormat("HH:mm:ss",Locale.ROOT).format(new Date(at)));}}
 @Override public void onResume(){super.onResume();handler.post(poll);}@Override public void onPause(){handler.removeCallbacks(poll);super.onPause();}
}

