package local.tclbrightness;
import android.content.Context;
import java.io.StringReader;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

public final class MenuControl {
  static final String PKG="com.android.tv.settings:id/";
  static Document ui(LocalAdb adb)throws Exception{
    String xml=adb.shell("uiautomator dump /data/local/tmp/tcl-brightness-ui.xml >/dev/null && cat /data/local/tmp/tcl-brightness-ui.xml");
    if(!xml.contains("<hierarchy")){Thread.sleep(1000);xml=adb.shell("uiautomator dump /data/local/tmp/tcl-brightness-ui.xml >/dev/null && cat /data/local/tmp/tcl-brightness-ui.xml");}
    int at=xml.indexOf("<?xml");if(at<0)at=xml.indexOf("<hierarchy");if(at<0)throw new IllegalStateException("UI tree unavailable");
    if(xml.contains("<!DOCTYPE")||xml.contains("<!ENTITY"))throw new IllegalStateException("Unexpected XML declaration");DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
    return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml.substring(at).trim())));
  }
  static String text(Document d,String id){NodeList nodes=d.getElementsByTagName("node");for(int i=0;i<nodes.getLength();i++){Element n=(Element)nodes.item(i);if(id.equals(n.getAttribute("resource-id")))return n.getAttribute("text");}return "";}
  static void require(Document d,String title){if(!title.equals(text(d,PKG+"page_title")))throw new IllegalStateException("Unexpected menu, expected "+title+", found "+text(d,PKG+"page_title"));}
  static int value(Document d){String text=text(d,PKG+"operate_text_value");if(text.isEmpty())text=text(d,PKG+"tv_element_item_progress_right_text");int n=Integer.parseInt(text);if(n<0||n>100)throw new IllegalStateException("Unexpected brightness value");return n;}
  static void keys(LocalAdb adb,int... keys)throws Exception{StringBuilder cmd=new StringBuilder("input keyevent");for(int key:keys)cmd.append(' ').append(key);adb.shell(cmd.toString());Thread.sleep(300);}
  static void clickText(LocalAdb adb,Document d,String wanted)throws Exception{NodeList nodes=d.getElementsByTagName("node");for(int i=0;i<nodes.getLength();i++){Element n=(Element)nodes.item(i);if(wanted.equals(n.getAttribute("text"))&&!n.getAttribute("resource-id").endsWith("page_title")){String[] parts=n.getAttribute("bounds").replaceAll("[\\[\\]]",",").split(",");java.util.ArrayList<Integer> v=new java.util.ArrayList<>();for(String part:parts)if(!part.isEmpty())v.add(Integer.parseInt(part));adb.shell("input tap "+((v.get(0)+v.get(2))/2)+" "+((v.get(1)+v.get(3))/2));Thread.sleep(350);return;}}throw new IllegalStateException("Menu entry unavailable: "+wanted);}
  public static synchronized int apply(Context c,int target)throws Exception{
    if(target<1||target>100)throw new IllegalArgumentException("Brightness must be 1-100");
    try(LocalAdb adb=new LocalAdb(c)){
      String activity=adb.shell("dumpsys activity activities");java.util.regex.Matcher task=java.util.regex.Pattern.compile("mResumedActivity:.*?\\su\\d+\\s+[A-Za-z0-9._$/]+\\s+t(\\d+)").matcher(activity);int previousTask=task.find()?Integer.parseInt(task.group(1)):-1;
      int depth=0;
      try{
        adb.shell("am start -W -n com.android.tv.settings/.MainSettings");depth=1;
        Document main=ui(adb);NodeList texts=main.getElementsByTagName("node");Element screen=null;
        for(int i=0;i<texts.getLength();i++){Element n=(Element)texts.item(i);if("Экран и звук".equals(n.getAttribute("text")))screen=n;}
        if(screen==null)throw new IllegalStateException("Display and sound entry unavailable");
        String[] bounds=screen.getAttribute("bounds").replaceAll("[\\[\\]]",",").split(",");java.util.ArrayList<Integer> coords=new java.util.ArrayList<>();for(String b:bounds)if(!b.isEmpty())coords.add(Integer.parseInt(b));
        int x=(coords.get(0)+coords.get(2))/2,y=(coords.get(1)+coords.get(3))/2;
        adb.shell("input tap "+x+" "+y);Thread.sleep(350);
        // Tap selects the left row; OK opens the TCL menu on this firmware.
        Document selected=ui(adb);if(!"Экран и звук".equals(text(selected,PKG+"page_title"))){keys(adb,23);selected=ui(adb);}depth=2;require(selected,"Экран и звук");
        clickText(adb,selected,"Изображение");depth=3;Document picture=ui(adb);require(picture,"Изображение");
        clickText(adb,picture,"Яркость");depth=4;Document brightness=ui(adb);require(brightness,"Яркость");
        int before=value(brightness);clickText(adb,brightness,"Яркость");depth=5;
        Document single=ui(adb);if(!"Яркость".equals(text(single,PKG+"operate_option_title")))throw new IllegalStateException("Brightness control not selected");
        int current=value(single);
        for(int round=0;round<3&&current!=target;round++){
          int count=Math.abs(target-current);int direction=target<current?21:22;
          while(count>0){int batch=Math.min(count,20);int[] k=new int[batch];java.util.Arrays.fill(k,direction);keys(adb,k);count-=batch;}
          current=value(ui(adb));
        }
        if(current!=target)throw new IllegalStateException("Brightness readback "+current+" differs from "+target);
        c.getSharedPreferences("schedule",0).edit().putInt("lastBefore",before).putInt("lastActual",current).putLong("lastSuccess",System.currentTimeMillis()).putString("lastError","").apply();
        android.util.Log.i("TCLBrightness","MENU_VERIFIED before="+before+" actual="+current);
        return current;
      }finally{if(depth>0){keys(adb,3);Thread.sleep(500);if(previousTask>0){String restored=adb.shell("env CLASSPATH='"+c.getApplicationInfo().sourceDir+"' app_process /system/bin local.tclbrightness.TaskRestore "+previousTask);if(!restored.contains("TASK_RESTORED="+previousTask))throw new IllegalStateException("Cannot restore previous screen: "+restored.trim());}}}
    }
  }
}
