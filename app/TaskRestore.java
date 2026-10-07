package local.tclbrightness;
public class TaskRestore {
 public static void main(String[] args)throws Exception{
  int task=Integer.parseInt(args[0]);if(task<=0)throw new IllegalArgumentException("Invalid task");
  Object service=Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null);
  Class<?> iface=Class.forName("android.app.IActivityTaskManager");java.lang.reflect.Method method=null;
  for(java.lang.reflect.Method m:iface.getMethods())if("moveTaskToFront".equals(m.getName())&&m.getParameterTypes().length==5)method=m;
  if(method==null)throw new IllegalStateException("Task restore API unavailable");
  method.invoke(service,null,"com.android.shell",task,2,null);
  System.out.println("TASK_RESTORED="+task);
 }
}
