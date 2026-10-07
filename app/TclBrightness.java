package local.tclbrightness;
import android.content.Context;
import java.lang.reflect.InvocationTargetException;

/** Uses the installed TCL shared library, subject to its normal permission checks. */
public final class TclBrightness {
  static Object manager() throws Exception {
    try {return Class.forName("com.tcl.tv.pq.TvBacklightManager").getMethod("getInstance").invoke(null);}
    catch(ClassNotFoundException e){throw new IllegalStateException("На этой прошивке нет библиотеки управления подсветкой TCL",e);}
    catch(InvocationTargetException e){throw failure(e);}
  }
  static Exception failure(InvocationTargetException e){return new IllegalStateException("TCL: "+e.getCause(),e.getCause());}
  static int read(Object manager) throws Exception {
    try {
      int level=(Integer)manager.getClass().getMethod("getBacklight").invoke(manager);
      if(level<0||level>100)throw new IllegalStateException("TCL не предоставил доступ к подсветке: "+level);
      return level;
    }catch(InvocationTargetException e){throw failure(e);}
  }
  public static synchronized int read() throws Exception {return read(manager());}
  public static synchronized int apply(Context c,int target) throws Exception {
    if(target<1||target>100)throw new IllegalArgumentException("Яркость должна быть от 1 до 100");
    Object manager=manager();int before=read(manager),actual=before;
    if(before!=target){
      Class<?> apply=Class.forName("com.tcl.tv.pq.EnApplyType"),act=Class.forName("com.tcl.tv.pq.EnTclActType");
      try {
        Object ok=manager.getClass().getMethod("setBacklight",apply,int.class,act).invoke(manager,
          apply.getField("EN_APPLY_CURRENT").get(null),target,act.getField("EN_TCL_ACT_EXEC_SAVE").get(null));
        if(!Boolean.TRUE.equals(ok))throw new IllegalStateException("TCL отклонил изменение подсветки");
      }catch(InvocationTargetException e){throw failure(e);}
      for(int n=0;n<10;n++){actual=read(manager);if(actual==target)break;Thread.sleep(100);}
      if(actual!=target)throw new IllegalStateException("Подсветка TCL: ожидалось "+target+", получено "+actual);
    }
    c.getSharedPreferences("schedule",0).edit().putInt("lastBefore",before).putInt("lastActual",actual)
      .putLong("lastSuccess",System.currentTimeMillis()).putString("lastError","").apply();
    android.util.Log.i("TCLBrightness","OEM_VERIFIED before="+before+" actual="+actual);
    return actual;
  }
}
