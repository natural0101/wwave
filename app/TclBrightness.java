package local.tclbrightness;
import android.content.Context;

/** Uses the installed TCL shared library, subject to its normal permission checks. */
public final class TclBrightness {
  private static NativeBacklight backend;
  static synchronized NativeBacklight backend(Context c)throws Exception{
    if(backend==null)backend=NativeBacklight.detect(Class::forName,Context.class,c.getApplicationContext());
    return backend;
  }
  public static synchronized int read(Context c)throws Exception{return backend(c).read();}
  public static synchronized String describe(Context c)throws Exception{return backend(c).name;}
  public static boolean writeVerified(Context c)throws Exception{
    String key=android.os.Build.FINGERPRINT+"|"+describe(c);
    return key.equals(c.getSharedPreferences("schedule",0).getString("verifiedDevice",""));
  }
  public static synchronized int apply(Context c,int target) throws Exception {
    NativeBacklight selected=backend(c);NativeBacklight.Change change=selected.change(target);
    c.getSharedPreferences("schedule",0).edit().putInt("lastBefore",change.before).putInt("lastActual",change.actual)
      .putLong("lastSuccess",System.currentTimeMillis()).putString("lastError","").apply();
    if(change.written)c.getSharedPreferences("schedule",0).edit().putString("verifiedDevice",android.os.Build.FINGERPRINT+"|"+selected.name).apply();
    android.util.Log.i("TCLBrightness","OEM_VERIFIED backend="+selected.name+" before="+change.before+" actual="+change.actual+" written="+change.written);
    return change.actual;
  }
}
