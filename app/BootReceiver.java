package local.tclbrightness;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){if(c.getSharedPreferences("schedule",0).getBoolean("enabled",false))ScheduleService.start(c,"wake");}}
