package local.tclbrightness;
/** Local wall-clock schedule; start is inclusive, end is exclusive. */
public final class ScheduleWindow {
  public static int minutes(String time){
    if(time==null||!time.matches("\\d{2}:\\d{2}"))throw new IllegalArgumentException("Формат времени: ЧЧ:ММ");
    String[] parts=time.split(":");int h=Integer.parseInt(parts[0]),m=Integer.parseInt(parts[1]);
    if(h>23||m>59)throw new IllegalArgumentException("Некорректное время");return h*60+m;
  }
  public static boolean night(int now,String start,String end){
    int s=minutes(start),e=minutes(end);
    if(s==e)throw new IllegalArgumentException("Время начала и окончания должно отличаться");
    return s>e?now>=s||now<e:now>=s&&now<e;
  }
}
