package local.tclbrightness;
import android.content.*;
public class AlarmReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){if(c.getSharedPreferences("schedule",0).getBoolean("enabled",false))try{ScheduleService.start(c,i.getStringExtra("op"));}catch(RuntimeException e){android.util.Log.e("TCLBrightness","ALARM_REJECTED",e);}}}
