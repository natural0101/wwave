package local.tclbrightness;

import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** PSK encrypted by this installation's Android Keystore key; excluded from backups. */
final class TvApiKey {
 static final String ALIAS="wwave-tv-api";
 static javax.crypto.SecretKey key()throws Exception{
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
  if(!ks.containsAlias(ALIAS)){
   KeyGenerator gen=KeyGenerator.getInstance("AES","AndroidKeyStore");
   gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();
  }
  return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);
 }
 static String read(Context c)throws Exception{
  SharedPreferences p=c.getSharedPreferences("schedule",0);String value=p.getString("apiKey","");if(value.isEmpty())return "";
  try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("apiIv",""),Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(value,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);}
  catch(Exception e){throw new IllegalStateException("Не удалось прочитать ключ IP control. Введите его снова");}
 }
 static void store(Context c,String kind,String value)throws Exception{
  if(value.length()>256||value.indexOf('\r')>=0||value.indexOf('\n')>=0)throw new IllegalArgumentException("Некорректный ключ IP control");
  String encoded="",iv="";
  if(!value.isEmpty()){
   Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
   encoded=Base64.encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);iv=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP);
  }
  if(!c.getSharedPreferences("schedule",0).edit().putString("driver",kind).putString("apiKey",encoded).putString("apiIv",iv).remove("verifiedDevice").remove("lastSuccess").remove("lastActual").putString("lastError","").putString("result","").commit())throw new IllegalStateException("Не удалось сохранить интерфейс телевизора");
 }
}
