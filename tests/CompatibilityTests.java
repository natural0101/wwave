package local.tclbrightness;
import java.util.*;
public class CompatibilityTests {
 static int checks=0;
 public enum Apply {EN_APPLY_CURRENT}
 public enum Act {EN_TCL_ACT_EXEC_SAVE}
 public static class FakeContext {}
 public static class Modern {
  static Modern instance=new Modern();int value=10,writes=0;String mode="normal";
  public static Modern getInstance(){return instance;}
  public int getBacklight(){return value;}
  public boolean setBacklight(Apply a,int level,Act b){
   writes++;
   if(a!=Apply.EN_APPLY_CURRENT||b!=Act.EN_TCL_ACT_EXEC_SAVE)throw new AssertionError("Wrong profile/action");
   if(mode.equals("ignore"))return true;
   if(mode.equals("deny"))return false;
   if(mode.equals("partial")&&level==20){value=20;return false;}
   if(mode.equals("restoreFails")){value=20;return false;}
   value=level;return true;
  }
 }
 public static class Legacy {
  static Legacy instance=new Legacy();int value=40,writes=0;
  public static Legacy getInstance(FakeContext c){if(c==null)throw new IllegalArgumentException("Missing context");return instance;}
  public int getBacklight(){return value;}
  public void setBacklight(int n){value=n;writes++;}
 }
 public static class Denied {
  public static Denied getInstance(){return new Denied();}
  public int getBacklight(){throw new SecurityException("OEM denies read");}
  public boolean setBacklight(Apply a,int n,Act b){throw new AssertionError("Must never write");}
 }
 public static class InvalidWriter {
  public static InvalidWriter getInstance(){return new InvalidWriter();}
  public int getBacklight(){return 30;}
  public String setBacklight(Apply a,int n,Act b){throw new AssertionError("Wrong API must never write");}
 }
 interface Action {void run()throws Exception;}
 static void yes(boolean b,String label){if(!b)throw new AssertionError(label);checks++;}
 static void fails(Action a,String text)throws Exception{try{a.run();throw new AssertionError("Expected failure: "+text);}catch(IllegalStateException|IllegalArgumentException e){yes(e.getMessage().contains(text),e.getMessage());}}
 static NativeBacklight.Classes classes(Class<?> modern,Class<?> legacy){return name->{
  if(name.equals("com.tcl.tv.pq.TvBacklightManager")&&modern!=null)return modern;
  if(name.equals("com.tcl.tv.pq.EnApplyType"))return Apply.class;
  if(name.equals("com.tcl.tv.pq.EnTclActType"))return Act.class;
  if(name.equals("com.tcl.tvmanager.TTvPictureManager")&&legacy!=null)return legacy;
  throw new ClassNotFoundException(name);
 };}
 static NativeBacklight detect(Class<?> modern,Class<?> legacy)throws Exception{return NativeBacklight.detect(classes(modern,legacy),FakeContext.class,new FakeContext());}
 static void fresh(){Modern.instance=new Modern();Legacy.instance=new Legacy();}
 public static void main(String[] args)throws Exception{
  fresh();NativeBacklight b=detect(Modern.class,null);
  yes(Modern.instance.writes==0,"Detection must be read-only");
  NativeBacklight.Change c=b.change(20);yes(c.before==10&&c.actual==20&&c.written,"Modern write/readback");
  yes(Modern.instance.writes==1,"Exactly one write");c=b.change(20);yes(!c.written&&Modern.instance.writes==1,"Same level never claims a tested write");
  fails(()->b.change(101),"от 1 до 100");yes(b.read()==20,"Invalid target unchanged");
  fresh();NativeBacklight legacy=detect(null,Legacy.class);yes(legacy.name.equals("TCL TVManager"),"Legacy selected");yes(legacy.change(25).actual==25,"Legacy void setter readback");
  fresh();NativeBacklight fallback=detect(Denied.class,Legacy.class);yes(fallback.name.equals("TCL TVManager")&&Legacy.instance.writes==0,"Read-only selection can try another documented OEM contract");
  fails(()->detect(null,null),"не поддерживается");
  fails(()->detect(Denied.class,null),"SecurityException");
  fails(()->detect(InvalidWriter.class,null),"setBacklight(): boolean");
  Modern.instance.value=-1;fails(()->detect(Modern.class,null),"достоверного");yes(Modern.instance.writes==0,"Invalid read blocks writes");
  NativeBacklight.Classes broken=name->{throw new NoClassDefFoundError("dependency missing");};
  fails(()->NativeBacklight.detect(broken,FakeContext.class,new FakeContext()),"NoClassDefFoundError");
  fresh();Modern.instance.mode="ignore";NativeBacklight ignored=detect(Modern.class,null);fails(()->ignored.change(20),"Исходная подсветка 10 восстановлена и проверена");yes(Modern.instance.value==10,"Ignored write not called success");
  fresh();Modern.instance.mode="partial";NativeBacklight partial=detect(Modern.class,null);fails(()->partial.change(20),"Исходная подсветка 10 восстановлена и проверена");yes(Modern.instance.value==10&&Modern.instance.writes==2,"Partial change rolled back");
  fresh();Modern.instance.mode="deny";NativeBacklight denied=detect(Modern.class,null);fails(()->denied.change(20),"отклонила");yes(Modern.instance.writes==1&&Modern.instance.value==10,"No second backend write after refusal");
  fresh();Modern.instance.mode="restoreFails";NativeBacklight noRollback=detect(Modern.class,null);fails(()->noRollback.change(20),"Восстановление исходной подсветки не подтверждено");
  fresh();Modern.instance.value=0;Modern.instance.mode="partial";NativeBacklight zero=detect(Modern.class,null);fails(()->zero.change(20),"Исходная подсветка 0 восстановлена и проверена");yes(Modern.instance.value==0,"Rollback preserves a valid zero original");
  yes(!ScheduleWindow.night(1259,"21:00","09:00"),"Before night");yes(ScheduleWindow.night(1260,"21:00","09:00"),"Night boundary");yes(ScheduleWindow.night(0,"21:00","09:00"),"Midnight");yes(ScheduleWindow.night(539,"21:00","09:00"),"Before morning");yes(!ScheduleWindow.night(540,"21:00","09:00"),"Morning boundary");yes(ScheduleWindow.night(720,"08:00","20:00"),"Same-day window");yes(!ScheduleWindow.night(1200,"08:00","20:00"),"Same-day end");fails(()->ScheduleWindow.night(720,"09:00","09:00"),"должно отличаться");fails(()->ScheduleWindow.minutes("24:00"),"Некорректное");fails(()->ScheduleWindow.minutes("09:60"),"Некорректное");
  System.out.println("PASS: "+checks+" compatibility, failure/rollback and schedule assertions. Synthetic OEM fixtures; not hardware certification.");
 }
}
