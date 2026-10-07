package local.tclbrightness;
import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.net.Socket;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.math.BigInteger;

public final class LocalAdb implements AutoCloseable {
  static final int CNXN=0x4e584e43,AUTH=0x48545541,OPEN=0x4e45504f,OKAY=0x59414b4f,WRTE=0x45545257,CLSE=0x45534c43;
  Socket socket;InputStream input;OutputStream output;int stream=0;
  static class Packet{int command,a,b;byte[] data;}
  // Each fresh installation owns its key. Existing installations retain their authorized key.
  static synchronized PrivateKey key(Context c)throws Exception{
    File file=new File(c.getFilesDir(),"adbkey");
    if(!file.exists()){
      KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(new RSAKeyGenParameterSpec(2048,RSAKeyGenParameterSpec.F4));
      PrivateKey generated=generator.generateKeyPair().getPrivate();
      String pem="-----BEGIN PRIVATE KEY-----\n"+Base64.encodeToString(generated.getEncoded(),Base64.NO_WRAP)+"\n-----END PRIVATE KEY-----\n";
      File temporary=new File(c.getFilesDir(),"adbkey.new");
      Files.write(temporary.toPath(),pem.getBytes(StandardCharsets.US_ASCII));
      if(!temporary.renameTo(file))throw new IOException("Cannot save application ADB key");
    }
    String pem=new String(Files.readAllBytes(file.toPath()),StandardCharsets.US_ASCII);
    pem=pem.replace("-----BEGIN PRIVATE KEY-----","").replace("-----END PRIVATE KEY-----","").replaceAll("\\s","");
    return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(pem,Base64.DEFAULT)));
  }
  static byte[] little(BigInteger n,int size){byte[] big=n.toByteArray(),result=new byte[size];for(int i=0;i<size&&i<big.length;i++)result[i]=big[big.length-1-i];return result;}
  static byte[] publicKey(PrivateKey key)throws Exception{
    RSAPrivateKeySpec spec=KeyFactory.getInstance("RSA").getKeySpec(key,RSAPrivateKeySpec.class);
    BigInteger modulus=spec.getModulus(),word=BigInteger.ONE.shiftLeft(32);
    int inverse=modulus.mod(word).modInverse(word).negate().mod(word).intValue();
    ByteBuffer data=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);
    data.putInt(64).putInt(inverse).put(little(modulus,256));
    // ADB and keys generated above use the standard public exponent 65537.
    if(modulus.bitLength()!=2048)throw new IOException("ADB requires a 2048-bit RSA key");
    data.put(little(BigInteger.ONE.shiftLeft(4096).mod(modulus),256)).putInt(65537);
    return (Base64.encodeToString(data.array(),Base64.NO_WRAP)+" ww@android\0").getBytes(StandardCharsets.US_ASCII);
  }
  public LocalAdb(Context c)throws Exception{
    PrivateKey key=key(c);
    socket=new Socket();socket.connect(new java.net.InetSocketAddress("127.0.0.1",5555),4000);socket.setSoTimeout(20000);
    input=socket.getInputStream();output=socket.getOutputStream();
    try{
    send(CNXN,0x01000000,4096,"host::\0".getBytes(StandardCharsets.UTF_8));
    boolean signed=false,published=false;
    for(int n=0;n<4;n++){
      Packet p=receive();if(p.command==CNXN)return;
      if(p.command!=AUTH||p.a!=1)throw new IOException("Unexpected ADB handshake");
      if(signed){
        if(published)throw new IOException("Разрешите подключение WWave в запросе отладки на телевизоре");
        socket.setSoTimeout(45000);send(AUTH,3,0,publicKey(key));published=true;continue;
      }
      byte[] prefix=new byte[]{0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};
      Signature signature=Signature.getInstance("NONEwithRSA");signature.initSign(key);signature.update(prefix);signature.update(p.data);
      send(AUTH,2,0,signature.sign());signed=true;
    }
    throw new IOException("ADB authentication did not complete");
    }catch(Exception e){try{close();}catch(IOException ignored){}throw e;}
  }
  void send(int cmd,int a,int b,byte[] data)throws IOException{
    int sum=0;for(byte v:data)sum+=v&255;
    byte[] header=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putInt(cmd).putInt(a).putInt(b).putInt(data.length).putInt(sum).putInt(cmd^0xffffffff).array();
    output.write(header);output.write(data);output.flush();
  }
  byte[] bytes(int count)throws IOException{byte[] data=new byte[count];int at=0;while(at<count){int n=input.read(data,at,count-at);if(n<0)throw new EOFException();at+=n;}return data;}
  Packet receive()throws IOException{
    ByteBuffer h=ByteBuffer.wrap(bytes(24)).order(ByteOrder.LITTLE_ENDIAN);Packet p=new Packet();p.command=h.getInt();p.a=h.getInt();p.b=h.getInt();int length=h.getInt();h.getInt();
    if(h.getInt()!=(p.command^0xffffffff)||length<0||length>1048576)throw new IOException("Invalid ADB packet");p.data=bytes(length);return p;
  }
  public String shell(String cmd)throws Exception{
    int id=++stream,remote=0;ByteArrayOutputStream result=new ByteArrayOutputStream();
    send(OPEN,id,0,("shell:"+cmd+"\0").getBytes(StandardCharsets.UTF_8));
    while(true){Packet p=receive();if(p.b!=id){if(p.b<id&&(p.command==CLSE||p.command==OKAY))continue;throw new IOException("Unexpected ADB stream cmd="+Integer.toHexString(p.command)+" remote="+p.a+" local="+p.b+" expected="+id);}remote=p.a;
      if(p.command==WRTE){result.write(p.data);send(OKAY,id,remote,new byte[0]);}
      else if(p.command==CLSE){send(CLSE,id,remote,new byte[0]);break;}
      else if(p.command!=OKAY)throw new IOException("Unexpected ADB response");
    }
    return result.toString("UTF-8");
  }
  public void close()throws IOException{if(socket!=null)socket.close();}
}
