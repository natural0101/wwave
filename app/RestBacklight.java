package local.tclbrightness;

import org.json.*;

/** Experimental implementations of published Sony BRAVIA / Hisense DS contracts. */
public final class RestBacklight implements NativeBacklight.Driver {
 public interface Transport {String request(int port,String method,String path,String header,String key,String body)throws Exception;}
 private final String kind,key;private final Transport http;private BacklightScale range;private int id=0;
 public RestBacklight(String kind,String key,Transport http)throws Exception{
  if(!kind.equals("sony")&&!kind.equals("hisense"))throw new IllegalArgumentException("Неизвестный интерфейс");
  this.kind=kind;this.key=key;this.http=http;
  if(kind.equals("sony"))checkSonyContract();
  readRaw(); // Discovery only reads; it never tests a write automatically.
 }
 public String name(){return kind.equals("sony")?"Sony BRAVIA REST · экспериментальный":"Hisense DS REST · экспериментальный";}
 public BacklightScale scale(){return range;}
 static int integer(Object v)throws Exception{
  if(!(v instanceof Number)&&!(v instanceof String))throw new IllegalStateException("Недостоверное число подсветки");
  double n;try{n=Double.parseDouble(v.toString());}catch(NumberFormatException e){throw new IllegalStateException("Недостоверное число подсветки");}
  if(!Double.isFinite(n)||n!=Math.rint(n)||n<Integer.MIN_VALUE||n>Integer.MAX_VALUE)throw new IllegalStateException("Недостоверное число подсветки");
  return (int)n;
 }
 JSONObject sony(String path,String method,JSONArray params)throws Exception{
  int requestId=++id;
  JSONObject body=new JSONObject().put("method",method).put("params",params).put("id",requestId).put("version","1.0");
  JSONObject response=new JSONObject(http.request(80,"POST",path,"X-Auth-PSK",key,body.toString()));
  if(response.has("error"))throw new IllegalStateException("Sony API отклонил запрос "+method);
  if(integer(response.get("id"))!=requestId||!(response.opt("result") instanceof JSONArray))throw new IllegalStateException("Неверный ответ Sony API");
  return response;
 }
 void checkSonyContract()throws Exception{
  JSONArray services=sony("/sony/guide","getSupportedApiInfo",new JSONArray().put(new JSONObject().put("services",new JSONArray().put("video")))).getJSONArray("result").getJSONArray(0);
  boolean read=false,write=false;
  for(int n=0;n<services.length();n++){
   JSONObject service=services.getJSONObject(n);if(!"video".equals(service.optString("service")))continue;
   JSONArray apis=service.getJSONArray("apis");for(int j=0;j<apis.length();j++){
    JSONObject api=apis.getJSONObject(j);JSONArray versions=api.getJSONArray("versions");boolean v1=false;
    for(int k=0;k<versions.length();k++)if("1.0".equals(versions.getJSONObject(k).optString("version")))v1=true;
    if(v1&&"getPictureQualitySettings".equals(api.optString("name")))read=true;
    if(v1&&"setPictureQualitySettings".equals(api.optString("name")))write=true;
   }
  }
  if(!read||!write)throw new IllegalStateException("На этой прошивке Sony нет пары API подсветки версии 1.0");
 }
 JSONObject hisense(String method,String path,JSONObject body)throws Exception{
  JSONObject response=new JSONObject(http.request(8091,method,path,"Auth-PSK",key,body==null?null:body.toString()));
  if(integer(response.get("code"))!=200||!Boolean.TRUE.equals(response.opt("isSuccess")))throw new IllegalStateException("Hisense API отклонил запрос подсветки");
  return response;
 }
 public synchronized int readRaw()throws Exception{
  int raw;BacklightScale next;
  if(kind.equals("sony")){
   JSONArray targets=sony("/sony/video","getPictureQualitySettings",new JSONArray().put(new JSONObject().put("target","brightness"))).getJSONArray("result").getJSONArray(0);
   if(targets.length()!=1)throw new IllegalStateException("Sony не вернул единственный параметр brightness");
   JSONObject target=targets.getJSONObject(0);
   if(!"brightness".equals(target.optString("target"))||(target.has("isAvailable")&&!Boolean.TRUE.equals(target.opt("isAvailable"))))throw new IllegalStateException("Подсветка Sony недоступна для текущего входа");
   JSONArray choices=target.getJSONArray("candidate");if(choices.length()!=1)throw new IllegalStateException("Неизвестная шкала подсветки Sony");
   JSONObject c=choices.getJSONObject(0);
   if(!c.optString("value","").isEmpty())throw new IllegalStateException("Подсветка Sony не имеет числовой шкалы");
   next=new BacklightScale(integer(c.get("min")),integer(c.get("max")),integer(c.get("step")));raw=integer(target.get("currentValue"));
  }else{next=new BacklightScale(0,100,1);raw=integer(hisense("GET","/hisense/source/getBackLightValue",null).get("data"));}
  next.check(raw);if(range!=null&&!range.equals(next))throw new IllegalStateException("Шкала подсветки изменилась. Остановите расписание и повторите проверку совместимости");
  range=next;return raw;
 }
 public synchronized void writeRaw(int raw)throws Exception{
  range.check(raw);
  if(kind.equals("sony")){
   JSONArray result=sony("/sony/video","setPictureQualitySettings",new JSONArray().put(new JSONObject().put("settings",new JSONArray().put(new JSONObject().put("target","brightness").put("value",Integer.toString(raw)))))).getJSONArray("result");
   if(result.length()!=0)throw new IllegalStateException("Неизвестный ответ записи Sony");
  }else if(!Boolean.TRUE.equals(hisense("POST","/hisense/source/setBackLightValue",new JSONObject().put("value",raw)).opt("data")))throw new IllegalStateException("Hisense отклонил изменение подсветки");
 }
}
