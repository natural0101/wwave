package local.tclbrightness;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){if(c.getSharedPreferences("schedule",0).getBoolean("enabled",false))try{ScheduleService.start(c,"wake");}catch(RuntimeException e){android.util.Log.e("TCLBrightness","RESUME_REJECTED",e);}}}
