package local.tclbrightness;
import android.content.*;
public class AlarmReceiver extends BroadcastReceiver {public void onReceive(Context c,Intent i){ScheduleService.start(c,i.getStringExtra("op"));}}
