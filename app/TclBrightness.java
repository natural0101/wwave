package local.tclbrightness;
import android.content.Context;

/** Connects only to the selected native TV interface. A failed write never switches drivers. */
public final class TclBrightness {
  private static NativeBacklight backend;
  static synchronized NativeBacklight backend(Context c)throws Exception{
    if(backend==null){
      String kind=c.getSharedPreferences("schedule",0).getString("driver","tcl");
      if("tcl".equals(kind))backend=NativeBacklight.detect(Class::forName,Context.class,c.getApplicationContext());
      else backend=NativeBacklight.connect(new RestBacklight(kind,TvApiKey.read(c),new LocalTvHttp()));
    }
    return backend;
  }
  static void invalidate(Context c){backend=null;c.getSharedPreferences("schedule",0).edit().remove("verifiedDevice").apply();}
  public static synchronized int read(Context c)throws Exception{try{return backend(c).read();}catch(Exception e){invalidate(c);throw e;}}
  public static synchronized String describe(Context c)throws Exception{return backend(c).name;}
  public static synchronized String nativeDetails(Context c)throws Exception{try{NativeBacklight b=backend(c);return "Шкала телевизора: "+b.scale()+". Считано: "+b.readRaw();}catch(Exception e){invalidate(c);throw e;}}
  public static synchronized void configure(Context c,String kind,String key)throws Exception{
    if(c.getSharedPreferences("schedule",0).getBoolean("enabled",false)||c.getSharedPreferences("schedule",0).getBoolean("working",false))throw new IllegalStateException("Сначала отключите расписание и дождитесь возврата дневной яркости");
    if(!kind.equals("tcl")&&!kind.equals("sony")&&!kind.equals("hisense"))throw new IllegalArgumentException("Неизвестный интерфейс");
    TvApiKey.store(c,kind,kind.equals("tcl")?"":key);backend=null;
  }
  public static boolean writeVerified(Context c)throws Exception{
    String key=android.os.Build.FINGERPRINT+"|"+describe(c)+"|"+backend(c).scale();
    return key.equals(c.getSharedPreferences("schedule",0).getString("verifiedDevice",""));
  }
  public static synchronized int apply(Context c,int target) throws Exception {
    NativeBacklight selected;NativeBacklight.Change change;
    try{selected=backend(c);change=selected.change(target);}catch(Exception e){invalidate(c);throw e;}
    c.getSharedPreferences("schedule",0).edit().putInt("lastBefore",change.before).putInt("lastActual",change.actual)
      .putLong("lastSuccess",System.currentTimeMillis()).putString("lastError","").apply();
    if(change.written)c.getSharedPreferences("schedule",0).edit().putString("verifiedDevice",android.os.Build.FINGERPRINT+"|"+selected.name+"|"+selected.scale()).apply();
    android.util.Log.i("TCLBrightness","OEM_VERIFIED backend="+selected.name+" before="+change.before+" actual="+change.actual+" written="+change.written);
    return change.actual;
  }
}
