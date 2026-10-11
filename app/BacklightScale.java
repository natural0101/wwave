package local.tclbrightness;

/** A documented native scale; percent is a UI value, readback uses the exact raw value. */
public final class BacklightScale {
 public final int min,max,step;
 public BacklightScale(int min,int max,int step){
  if(min<0||max<=min||max>10000||step<1||(max-min)%step!=0)throw new IllegalArgumentException("Недостоверная шкала подсветки");
  this.min=min;this.max=max;this.step=step;
 }
 public int raw(int percent){
  if(percent<1||percent>100)throw new IllegalArgumentException("Яркость должна быть от 1 до 100");
  int ticks=(max-min)/step;
  return min+Math.max(1,Math.min(ticks,(int)Math.round(percent*ticks/100.0)))*step;
 }
 public int percent(int raw){check(raw);return (int)Math.round(100.0*(raw-min)/(max-min));}
 public void check(int raw){if(raw<min||raw>max||(raw-min)%step!=0)throw new IllegalStateException("Нет достоверного уровня подсветки: "+raw);}
 public boolean equals(Object o){if(!(o instanceof BacklightScale))return false;BacklightScale s=(BacklightScale)o;return min==s.min&&max==s.max&&step==s.step;}
 public int hashCode(){return (min*31+max)*31+step;}
 public String toString(){return min+"–"+max+", шаг "+step;}
}
