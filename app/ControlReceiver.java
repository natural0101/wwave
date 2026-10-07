package local.tclbrightness;
import android.content.*;
public class ControlReceiver extends BroadcastReceiver {
 public void onReceive(Context c,Intent i){final PendingResult pending=goAsync();final String op=i.getStringExtra("op");new Thread(()->{try{String result;
  if("connection".equals(op)){try(LocalAdb adb=new LocalAdb(c)){result=adb.shell("id").trim();}}
  else if("set".equals(op)){result="MENU_ACTUAL="+MenuControl.apply(c,i.getIntExtra("level",-1));}
  else if("status".equals(op)){result=c.getSharedPreferences("schedule",0).getAll().toString();}
  else if("testTimers".equals(op)){ScheduleService.testTimers(c);result="Test alarms scheduled";}
  else{ScheduleService.start(c,op);result="Action requested: "+op;}
  pending.setResultCode(0);pending.setResultData(result);android.util.Log.i("TCLBrightness",result);
 }catch(Exception e){pending.setResultCode(1);pending.setResultData(e.toString());android.util.Log.e("TCLBrightness","CONTROL_FAILED",e);}finally{pending.finish();}}).start();}
}
