package com.bloodbridge;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class AdminCredentials {
 public final String username;private final String passwordHash;
 public AdminCredentials(@Value("${bloodbridge.admin-username:}") String username,@Value("${bloodbridge.admin-password-hash:}") String hash){this.username=username;passwordHash=hash;}
 public boolean configured(){try{String[] p=passwordHash.split(":");int i=Integer.parseInt(p[1]);return !username.isBlank()&&p.length==4&&p[0].equals("pbkdf2")&&i>=210000&&i<=1000000&&Base64.getDecoder().decode(p[2]).length==16&&Base64.getDecoder().decode(p[3]).length==32;}catch(Exception e){return false;}}
 public boolean accepts(String id,String password){try{boolean ready=configured();String[] p=ready?passwordHash.split(":"):new String[]{"pbkdf2","210000","AAAAAAAAAAAAAAAAAAAAAA==","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="};byte[] actual=derive(password,Base64.getDecoder().decode(p[2]),Integer.parseInt(p[1]));boolean match=MessageDigest.isEqual(actual,Base64.getDecoder().decode(p[3]));return ready&&MessageDigest.isEqual(username.getBytes(StandardCharsets.UTF_8),id.getBytes(StandardCharsets.UTF_8))&&match;}catch(Exception e){return false;}}
 static byte[] derive(String password,byte[] salt,int iterations) throws Exception {PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),salt,iterations,256);try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}}
}
