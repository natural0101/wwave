package local.tclbrightness;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Known OEM contracts only. No Android brightness setting or overlay fallback. */
public final class NativeBacklight {
  public interface Classes {Class<?> load(String name) throws ClassNotFoundException;}
  private final Object manager, applyCurrent, executeSave;
  private final Method getter, setter;
  public final String name;

  private NativeBacklight(String name,Object manager,Method getter,Method setter,Object current,Object save){
    this.name=name;this.manager=manager;this.getter=getter;this.setter=setter;
    applyCurrent=current;executeSave=save;
  }
  static Object call(Method method,Object target,Object... args)throws Exception{
    try{return method.invoke(target,args);}
    catch(InvocationTargetException e){
      Throwable cause=e.getCause();
      throw new IllegalStateException("Интерфейс подсветки: "+cause.getClass().getSimpleName()+": "+cause.getMessage(),cause);
    }catch(LinkageError e){throw new IllegalStateException("Несовместимая библиотека подсветки: "+e.getClass().getSimpleName(),e);}
  }
  public static NativeBacklight detect(Classes classes,Class<?> contextType,Object context)throws Exception{
    StringBuilder failures=new StringBuilder();
    for(int kind=0;kind<2;kind++){
      String label=kind==0?"TCL PQ SDK":"TCL TVManager";
      try{
        Class<?> cls=classes.load(kind==0?"com.tcl.tv.pq.TvBacklightManager":"com.tcl.tvmanager.TTvPictureManager");
        Method get=cls.getMethod("getBacklight");
        if(get.getReturnType()!=int.class)throw new NoSuchMethodException("getBacklight(): int");
        Object manager,current=null,save=null;Method set;
        if(kind==0){
          Class<?> a=classes.load("com.tcl.tv.pq.EnApplyType"),s=classes.load("com.tcl.tv.pq.EnTclActType");
          current=a.getField("EN_APPLY_CURRENT").get(null);save=s.getField("EN_TCL_ACT_EXEC_SAVE").get(null);
          set=cls.getMethod("setBacklight",a,int.class,s);
          if(set.getReturnType()!=boolean.class)throw new NoSuchMethodException("setBacklight(): boolean");
          manager=call(cls.getMethod("getInstance"),null);
        }else{
          set=cls.getMethod("setBacklight",int.class);
          if(set.getReturnType()!=void.class&&set.getReturnType()!=boolean.class)throw new NoSuchMethodException("setBacklight(): void/boolean");
          manager=call(cls.getMethod("getInstance",contextType),null,context);
        }
        if(manager==null)throw new IllegalStateException("Служба недоступна");
        NativeBacklight backend=new NativeBacklight(label,manager,get,set,current,save);
        backend.read(); // Detection never writes or changes picture profiles.
        return backend;
      }catch(Exception|LinkageError e){
        if(failures.length()>0)failures.append("; ");
        failures.append(label).append(": ").append(e.getClass().getSimpleName());
        if(!(e instanceof ClassNotFoundException)&&e.getMessage()!=null)failures.append(" (").append(e.getMessage()).append(')');
      }
    }
    throw new IllegalStateException("Управление аппаратной подсветкой этой прошивки не поддерживается. "+failures);
  }
  public int read()throws Exception{
    Object value=call(getter,manager);
    if(!(value instanceof Integer))throw new IllegalStateException("Неверный ответ подсветки");
    int n=(Integer)value;
    if(n<0||n>100)throw new IllegalStateException("Нет достоверного уровня подсветки: "+n);
    return n;
  }
  void write(int target)throws Exception{
    Object result=applyCurrent==null?call(setter,manager,target):call(setter,manager,applyCurrent,target,executeSave);
    if(setter.getReturnType()==boolean.class&&!Boolean.TRUE.equals(result))throw new IllegalStateException("Прошивка отклонила изменение подсветки");
  }
  int await(int expected)throws Exception{
    int actual=read();
    for(int n=0;n<10&&actual!=expected;n++){Thread.sleep(100);actual=read();}
    if(actual!=expected)throw new IllegalStateException("Ожидалось "+expected+", получено "+actual);
    return actual;
  }
  public static final class Change {
    public final int before,actual;public final boolean written;
    Change(int before,int actual,boolean written){this.before=before;this.actual=actual;this.written=written;}
  }
  public synchronized Change change(int target)throws Exception{
    if(target<1||target>100)throw new IllegalArgumentException("Яркость должна быть от 1 до 100");
    int before=read();
    if(before==target)return new Change(before,before,false);
    try{write(target);return new Change(before,await(target),true);}
    catch(Exception failed){
      String rollback;
      try{if(read()!=before)write(before);await(before);rollback="Исходная подсветка "+before+" восстановлена и проверена.";}
      catch(Exception restore){rollback="Восстановление исходной подсветки не подтверждено: "+restore.getMessage();}
      throw new IllegalStateException(failed.getMessage()+". "+rollback,failed);
    }
  }
}
