package com.bloodbridge;
import com.fasterxml.jackson.databind.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class SupabaseAuthProvider {
 private final String base,key,site;
 private final ObjectMapper json;
 private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NEVER).build();
 public record Identity(String id,String name,String email,String phone,String provider,int expiresIn) {}
 public SupabaseAuthProvider(@Value("${bloodbridge.supabase-url:}") String base,@Value("${bloodbridge.supabase-key:}") String key,@Value("${bloodbridge.site-origin:http://localhost:8080}") String site,ObjectMapper json){this.base=base.replaceAll("/+$","");this.key=key;this.site=site.replaceAll("/+$","");this.json=json;}
 public boolean configured(){try{URI b=URI.create(base),s=URI.create(site);return !key.isBlank()&&"https".equals(b.getScheme())&&b.getHost()!=null&&b.getHost().endsWith(".supabase.co")&&b.getUserInfo()==null&&(b.getPath()==null||b.getPath().isEmpty())&&b.getQuery()==null&&b.getFragment()==null&&s.getHost()!=null&&s.getUserInfo()==null&&(s.getPath()==null||s.getPath().isEmpty())&&s.getQuery()==null&&s.getFragment()==null&&("https".equals(s.getScheme())||("http".equals(s.getScheme())&&"localhost".equals(s.getHost())));}catch(Exception e){return false;}}
 public static String randomToken(){byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
 public static String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
 public String authorizeUrl(String verifier){
  requireConfigured();
  try{String challenge=Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.UTF_8)));return base+"/auth/v1/authorize?provider=google&redirect_to="+URLEncoder.encode(site+"/api/auth/callback",StandardCharsets.UTF_8)+"&code_challenge="+challenge+"&code_challenge_method=s256";}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
 }
 public void sendPhone(String phone){call("otp",Map.of("phone",phone,"create_user",true,"channel","sms"));}
 public Identity verifyPhone(String phone,String token){JsonNode d=call("verify",Map.of("phone",phone,"token",token,"type","sms"));if(!d.path("user").path("phone").asText().replaceFirst("^\\+","").equals(phone.substring(1)))throw denied();return identity(d,"phone");}
 public Identity verifyGoogle(String code,String verifier){return identity(call("token?grant_type=pkce",Map.of("auth_code",code,"code_verifier",verifier)),"google");}
 private Identity identity(JsonNode d,String method){
  JsonNode u=d.path("user");if(d.path("access_token").asText().isBlank()||u.path("id").asText().isBlank())throw denied();
  if(method.equals("phone")&&u.path("phone_confirmed_at").asText("").isBlank())throw denied();
  if(method.equals("google")){boolean google=false;for(JsonNode i:u.path("identities"))if(i.path("provider").asText().equals("google"))google=true;if(!google||u.path("email_confirmed_at").asText("").isBlank())throw denied();}
  String name=u.path("user_metadata").path("full_name").asText(u.path("user_metadata").path("name").asText("Donor"));if(name.isBlank())name="Donor";
  return new Identity(u.path("id").asText(),name.substring(0,Math.min(80,name.length())),u.path("email").asText(""),u.path("phone").asText(""),method,Math.max(1,Math.min(3600,d.path("expires_in").asInt(3600))));
 }
 private JsonNode call(String path,Object body){
  requireConfigured();
  try{HttpRequest r=HttpRequest.newBuilder(URI.create(base+"/auth/v1/"+path)).timeout(Duration.ofSeconds(12)).header("apikey",key).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();HttpResponse<String> res=client.send(r,HttpResponse.BodyHandlers.ofString());if(res.statusCode()==429)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please wait before requesting another verification code.");if(res.statusCode()<200||res.statusCode()>=300)throw new ResponseStatusException(res.statusCode()>=500?HttpStatus.SERVICE_UNAVAILABLE:HttpStatus.BAD_REQUEST,"Sign-in could not be completed. Check your code or try again.");return json.readTree(res.body());}
  catch(ResponseStatusException e){throw e;}catch(InterruptedException e){Thread.currentThread().interrupt();throw unavailable();}catch(Exception e){throw unavailable();}
 }
 private void requireConfigured(){if(!configured())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Google and phone sign-in are awaiting setup.");}
 private ResponseStatusException denied(){return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"The sign-in provider did not verify this account.");}
 private ResponseStatusException unavailable(){return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"The sign-in provider is temporarily unavailable.");}
}
