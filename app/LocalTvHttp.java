package local.tclbrightness;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Only the TV's own loopback server. No discovery, arbitrary hosts, redirects or TLS bypass. */
public final class LocalTvHttp implements RestBacklight.Transport {
 public String request(int port,String method,String path,String header,String key,String body)throws Exception{
  if((port!=80&&port!=8091)||!(path.equals("/sony/guide")||path.equals("/sony/video")||path.equals("/hisense/source/getBackLightValue")||path.equals("/hisense/source/setBackLightValue")))throw new IllegalArgumentException("Неизвестный интерфейс телевизора");
  if(key.indexOf('\r')>=0||key.indexOf('\n')>=0)throw new IllegalArgumentException("Некорректный ключ IP control");
  HttpURLConnection c=(HttpURLConnection)new URL("http://127.0.0.1:"+port+path).openConnection(Proxy.NO_PROXY);
  c.setConnectTimeout(1000);c.setReadTimeout(1500);c.setInstanceFollowRedirects(false);c.setUseCaches(false);c.setRequestMethod(method);
  c.setRequestProperty("Accept","application/json");
  if(!key.isEmpty())c.setRequestProperty(header,key);
  try{
   if(body!=null){byte[] bytes=body.getBytes(StandardCharsets.UTF_8);c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=c.getOutputStream()){out.write(bytes);}}
   int status=c.getResponseCode();
   if(status==401||status==403)throw new IllegalStateException("Телевизор отказал в доступе. Проверьте IP control и ключ PSK");
   if(status!=200)throw new IllegalStateException("Локальный API телевизора: HTTP "+status);
   try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] bytes=new byte[4096];int total=0,n;while((n=in.read(bytes))!=-1){total+=n;if(total>65536)throw new IllegalStateException("Слишком большой ответ телевизора");out.write(bytes,0,n);}
    return new String(out.toByteArray(),StandardCharsets.UTF_8);
   }
  }catch(SocketTimeoutException|ConnectException e){throw new IllegalStateException("Локальный API недоступен. Включите IP control на телевизоре; эта прошивка может не поддерживать локальное подключение",e);}
  finally{c.disconnect();}
 }
}
