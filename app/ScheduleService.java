package local.tclbrightness;
import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.*;
public class ScheduleService extends Service {
 Handler h=new Handler();ExecutorService jobs=Executors.newSingleThreadExecutor();int inFlight=0;
 SharedPreferences p(){return getSharedPreferences("schedule",0);}
 public static void start(Context c,String op){
  try{
   if("start".equals(op)&&!canSchedule(c))throw new IllegalStateException("Разрешите WWave точные будильники в настройках телевизора");
   Intent i=new Intent(c,ScheduleService.class);i.putExtra("op",op);c.startForegroundService(i);
  }catch(RuntimeException e){
   c.getSharedPreferences("schedule",0).edit().putBoolean("enabled",false).putBoolean("working",false).putString("result","Запуск расписания не выполнен: "+e.getMessage()).putString("lastError",e.toString()).apply();
   cancel(c);throw e;
  }
 }
 public static int minutes(String time){return ScheduleWindow.minutes(time);}
 public static boolean canSchedule(Context c){return Build.VERSION.SDK_INT<31||((AlarmManager)c.getSystemService(ALARM_SERVICE)).canScheduleExactAlarms();}
 boolean nightNow(){Calendar n=Calendar.getInstance();return ScheduleWindow.night(n.get(Calendar.HOUR_OF_DAY)*60+n.get(Calendar.MINUTE),p().getString("start","21:00"),p().getString("end","09:00"));}
 static PendingIntent alarm(Context c,int id,String op){Intent i=new Intent(c,AlarmReceiver.class);i.putExtra("op",op);return PendingIntent.getBroadcast(c,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 static void next(Context c,int id,String time){int m=minutes(time);Calendar n=Calendar.getInstance();n.set(Calendar.HOUR_OF_DAY,m/60);n.set(Calendar.MINUTE,m%60);n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);if(n.getTimeInMillis()<=System.currentTimeMillis())n.add(Calendar.DATE,1);((AlarmManager)c.getSystemService(ALARM_SERVICE)).setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,n.getTimeInMillis(),alarm(c,id,"tick"));}
 void schedule(){if(!canSchedule(this))throw new IllegalStateException("Разрешите WWave точные будильники в настройках телевизора");next(this,21,p().getString("start","21:00"));next(this,9,p().getString("end","09:00"));}
 static void cancel(Context c){AlarmManager a=(AlarmManager)c.getSystemService(ALARM_SERVICE);for(int id:new int[]{21,9,101,102})a.cancel(alarm(c,id,"tick"));}
 public static void testTimers(Context c){if(!canSchedule(c))throw new IllegalStateException("Нет разрешения на точные будильники");AlarmManager a=(AlarmManager)c.getSystemService(ALARM_SERVICE);long n=System.currentTimeMillis();a.setExact(AlarmManager.RTC_WAKEUP,n+15000,alarm(c,101,"night"));a.setExact(AlarmManager.RTC_WAKEUP,n+55000,alarm(c,102,"day"));}
 void failure(Exception e){p().edit().putBoolean("enabled",false).putString("result",e.getMessage()).putString("lastError",e.toString()).apply();cancel(this);android.util.Log.e("TCLBrightness","SCHEDULE_FAILED",e);if(inFlight==0)stopSelf();}
 BroadcastReceiver wake=new BroadcastReceiver(){public void onReceive(Context c,Intent i){if(p().getBoolean("enabled",false)){try{evaluate(true);}catch(Exception e){failure(e);}}}};
 Runnable tick=new Runnable(){public void run(){if(p().getBoolean("enabled",false)){try{if(!canSchedule(ScheduleService.this))throw new IllegalStateException("Разрешение на точные будильники отозвано");evaluate(false);}catch(Exception e){failure(e);}}h.postDelayed(this,60000);}};
 public void onCreate(){super.onCreate();NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);nm.createNotificationChannel(new NotificationChannel("schedule","Расписание яркости",NotificationManager.IMPORTANCE_LOW));startForeground(1,new Notification.Builder(this,"schedule").setContentTitle("WWave").setContentText("Ночь "+p().getString("start","21:00")+"–"+p().getString("end","09:00")).setSmallIcon(android.R.drawable.ic_menu_recent_history).build());IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_ON);if(Build.VERSION.SDK_INT>=33)registerReceiver(wake,filter,4);else registerReceiver(wake,filter);h.postDelayed(tick,60000);}
 void evaluate(boolean force){if(!((PowerManager)getSystemService(POWER_SERVICE)).isInteractive())return;String phase=nightNow()?"night":"day";if(!force&&phase.equals(p().getString("phase","")))return;apply(phase,true);}
 void apply(String phase,boolean scheduled){if(inFlight>0&&scheduled)return;inFlight++;p().edit().putBoolean("working",true).apply();int level=p().getInt(phase,phase.equals("night")?10:30);jobs.submit(()->{try{int actual=TclBrightness.apply(this,level);p().edit().putString("result",phase+": "+actual+" / 100, "+(TclBrightness.writeVerified(this)?"запись и чтение подтверждены":"уровень считан; запись ещё не проверена")).putLong("lastRun",System.currentTimeMillis()).putString("lastError","").apply();if(scheduled)p().edit().putString("phase",phase).apply();android.util.Log.i("TCLBrightness","SCHEDULE_VERIFIED phase="+phase+" actual="+actual);}catch(Exception e){h.post(()->failure(e));}finally{h.post(()->{inFlight--;p().edit().putBoolean("working",inFlight>0).apply();if(inFlight==0&&!p().getBoolean("enabled",false))stopSelf();});}});}
 public int onStartCommand(Intent i,int f,int id){String op=i==null?"tick":i.getStringExtra("op");try{if("start".equals(op)){TclBrightness.read(this);nightNow();schedule();p().edit().putBoolean("enabled",true).putString("phase","").apply();evaluate(true);}else if("stop".equals(op)){p().edit().putBoolean("enabled",false).apply();cancel(this);apply("day",false);}else if("night".equals(op)||"day".equals(op)){apply(op,false);}else if(p().getBoolean("enabled",false)){TclBrightness.read(this);schedule();evaluate("wake".equals(op));}else stopSelf();}catch(Exception e){failure(e);}return START_STICKY;}
 public void onDestroy(){h.removeCallbacksAndMessages(null);unregisterReceiver(wake);jobs.shutdown();super.onDestroy();}public IBinder onBind(Intent i){return null;}
}
