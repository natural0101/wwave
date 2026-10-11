package local.tclbrightness;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class RestTests {
 static int checks;
 static void yes(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 interface Action {void run()throws Exception;}
 static void fails(Action a,String text)throws Exception{try{a.run();throw new AssertionError("Expected: "+text);}catch(Exception e){yes(e.getMessage()!=null&&e.getMessage().contains(text),e.toString());}}
 static final class Fake implements RestBacklight.Transport {
  int value=20,writes,reads,max=50,step=1;boolean available=true,ignore=false,refuse=false,error=false,missing=false,badId=false;String kind;
  Fake(String kind){this.kind=kind;}
  public String request(int port,String method,String path,String header,String key,String body)throws Exception{
   yes("fixture-key".equals(key),"Pass configured key to own transport");
   if(kind.equals("sony")){
    yes(port==80&&method.equals("POST")&&header.equals("X-Auth-PSK"),"Sony transport contract");
    JSONObject request=new JSONObject(body);yes(request.getString("version").equals("1.0"),"Version 1.0");
    String op=request.getString("method");JSONArray params=request.getJSONArray("params");JSONArray result=new JSONArray();
    if(op.equals("getSupportedApiInfo")){
     yes(path.equals("/sony/guide")&&params.getJSONObject(0).getJSONArray("services").getString(0).equals("video"),"Guide discovery");
     JSONArray apis=new JSONArray();for(String n:new String[]{"getPictureQualitySettings","setPictureQualitySettings"})if(!(missing&&n.startsWith("set")))apis.put(new JSONObject().put("name",n).put("versions",new JSONArray().put(new JSONObject().put("version","1.0"))));
     result.put(new JSONArray().put(new JSONObject().put("service","video").put("apis",apis)));
    }else if(op.equals("getPictureQualitySettings")){
     yes(path.equals("/sony/video")&&params.getJSONObject(0).getString("target").equals("brightness"),"Read exactly panel brightness");reads++;
     result.put(new JSONArray().put(new JSONObject().put("target","brightness").put("isAvailable",available).put("currentValue",Integer.toString(value)).put("candidate",new JSONArray().put(new JSONObject().put("min",0).put("max",max).put("step",step)))));
    }else if(op.equals("setPictureQualitySettings")){
     yes(path.equals("/sony/video"),"Write video endpoint");JSONArray settings=params.getJSONObject(0).getJSONArray("settings");yes(settings.length()==1&&settings.getJSONObject(0).getString("target").equals("brightness"),"Never write picture/color/HDR settings");writes++;if(!ignore)value=Integer.parseInt(settings.getJSONObject(0).getString("value"));
    }else throw new AssertionError(op);
    JSONObject response=new JSONObject().put("id",request.getInt("id")+(badId?1:0)).put("result",result);if(error||refuse&&op.equals("setPictureQualitySettings")&&value!=20)response.put("error",new JSONArray().put(403).put("denied"));return response.toString();
   }
   yes(port==8091&&header.equals("Auth-PSK"),"Hisense transport contract");
   if(method.equals("GET")){yes(path.equals("/hisense/source/getBackLightValue")&&body==null,"Hisense getter");reads++;}
   else{yes(method.equals("POST")&&path.equals("/hisense/source/setBackLightValue"),"Hisense writer");writes++;if(!ignore&&!refuse)value=new JSONObject(body).getInt("value");}
   return new JSONObject().put("code",error?401:200).put("isSuccess",!error).put("data",method.equals("GET")?Integer.valueOf(value):Boolean.valueOf(!refuse)).toString();
  }
 }
 public static void main(String[] args)throws Exception{
  BacklightScale fifty=new BacklightScale(0,50,1);yes(fifty.raw(31)==16&&fifty.percent(16)==32,"Quantized raw target, not requested percent");yes(fifty.raw(1)==1,"Do not round positive brightness to panel zero");
  BacklightScale two=new BacklightScale(0,100,2);yes(two.raw(31)==32,"Native step");fails(()->two.check(31),"достоверного");fails(()->new BacklightScale(0,51,2),"шкала");fails(()->new BacklightScale(0,0,1),"шкала");
  Fake sony=new Fake("sony");NativeBacklight b=NativeBacklight.connect(new RestBacklight("sony","fixture-key",sony));yes(sony.writes==0&&b.read()==40,"Discovery is read-only");NativeBacklight.Change change=b.change(31);yes(change.before==40&&change.actual==32&&change.written&&sony.value==16,"Scale50 exact raw write/readback");int writes=sony.writes;yes(!b.change(32).written&&sony.writes==writes,"Quantized same raw level is not a write");
  sony.max=100;fails(()->b.read(),"Шкала подсветки изменилась");yes(sony.writes==writes,"Profile scale drift blocks writing");
  Fake missing=new Fake("sony");missing.missing=true;fails(()->new RestBacklight("sony","fixture-key",missing),"нет пары API");yes(missing.writes==0,"Missing setter cannot be selected");
  Fake unavailable=new Fake("sony");unavailable.available=false;fails(()->new RestBacklight("sony","fixture-key",unavailable),"недоступна");
  Fake bad=new Fake("sony");bad.badId=true;fails(()->new RestBacklight("sony","fixture-key",bad),"Неверный ответ");
  Fake denied=new Fake("sony");NativeBacklight deny=NativeBacklight.connect(new RestBacklight("sony","fixture-key",denied));denied.refuse=true;fails(()->deny.change(30),"Исходная подсветка 20 восстановлена и проверена");yes(denied.writes==2&&denied.value==20,"Partial Sony error rolls back same driver");
  Fake hisense=new Fake("hisense");NativeBacklight h=NativeBacklight.connect(new RestBacklight("hisense","fixture-key",hisense));yes(hisense.writes==0,"Hisense detect read-only");yes(h.change(30).actual==30&&hisense.value==30,"Hisense contract writes and reads native value");
  hisense.refuse=true;fails(()->h.change(10),"Исходная подсветка 30 восстановлена и проверена");yes(hisense.value==30,"Boolean false is failure");
  Fake ignored=new Fake("hisense");ignored.ignore=true;NativeBacklight ig=NativeBacklight.connect(new RestBacklight("hisense","fixture-key",ignored));fails(()->ig.change(40),"Ожидалось 40, получено 20");yes(ignored.value==20,"Successful HTTP alone cannot verify writing");
  Fake auth=new Fake("hisense");auth.error=true;fails(()->new RestBacklight("hisense","fixture-key",auth),"отклонил");
  fails(()->RestBacklight.integer("NaN"),"Недостоверное");fails(()->RestBacklight.integer("1.5"),"Недостоверное");fails(()->RestBacklight.integer(Boolean.TRUE),"Недостоверное");
  LocalTvHttp transport=new LocalTvHttp();fails(()->transport.request(8091,"GET","/other","Auth-PSK","",null),"Неизвестный");fails(()->transport.request(8091,"GET","/hisense/source/getBackLightValue","Auth-PSK","injected\r\n",null),"Некорректный");
  // A real loopback HTTP fixture exercises redirects and authentication refusal without touching a TV.
  try(ServerSocket server=new ServerSocket(8091,1,InetAddress.getByName("127.0.0.1"))){
   Thread fixture=new Thread(()->{try{for(String status:new String[]{"302 Found","403 Forbidden","200 OK"})try(Socket client=server.accept()){
    BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream(),StandardCharsets.UTF_8));String line;while((line=reader.readLine())!=null&&!line.isEmpty()){}String payload=status.startsWith("200")?"{\"code\":200}":"";
    client.getOutputStream().write(("HTTP/1.1 "+status+"\r\nLocation: http://127.0.0.1:8091/other\r\nContent-Length: "+payload.length()+"\r\nConnection: close\r\n\r\n"+payload).getBytes(StandardCharsets.UTF_8));
   }}catch(Exception e){throw new RuntimeException(e);}});fixture.setDaemon(true);fixture.start();
   fails(()->transport.request(8091,"GET","/hisense/source/getBackLightValue","Auth-PSK","fixture-key",null),"HTTP 302");
   fails(()->transport.request(8091,"GET","/hisense/source/getBackLightValue","Auth-PSK","fixture-key",null),"отказал");
   yes(new JSONObject(transport.request(8091,"GET","/hisense/source/getBackLightValue","Auth-PSK","fixture-key",null)).getInt("code")==200,"Bounded real HTTP response");fixture.join(1000);
  }
  System.out.println("PASS: "+checks+" REST/scale/transport assertions. Protocol fixtures, not Sony/Hisense hardware certification.");
 }
}
