package com.bloodbridge;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api")
public class AuthController {
 private final AccountRepository accounts;private final DonorRepository donors;private final SupabaseAuthProvider provider;private final SecurityContextRepository contexts;private final SessionAuthenticationStrategy sessions;private final RateLimiter limits;private final ProgramSettings program;private final DeviceAccessRepository devices;private final AdminCredentials adminCredentials;
 public AuthController(AccountRepository a,DonorRepository d,SupabaseAuthProvider p,SecurityContextRepository c,SessionAuthenticationStrategy s,RateLimiter l,ProgramSettings program,DeviceAccessRepository devices,AdminCredentials credentials){accounts=a;donors=d;provider=p;contexts=c;sessions=s;limits=l;this.program=program;this.devices=devices;adminCredentials=credentials;}
 public record EmailEntry(@NotBlank @Email @Size(max=180) String email){}
 public record AdminEntry(@NotBlank @Size(max=80) String username,@NotBlank @Size(max=72) String password){}
 @PostMapping("/auth/email") public Map<String,Boolean> email(@Valid @RequestBody EmailEntry body,HttpServletRequest req,HttpServletResponse res){
  limits.check("entry:"+req.getRemoteAddr(),30,3600000);String email=body.email().trim().toLowerCase(Locale.ROOT);Account account=null;
  if(req.getCookies()!=null)for(Cookie cookie:req.getCookies())if(cookie.getName().equals("bb_device")&&cookie.getValue().matches("[a-zA-Z0-9_-]{43}")){
   DeviceAccess access=devices.findById(SupabaseAuthProvider.hash(cookie.getValue())).orElse(null);
   if(access!=null&&access.expiresAt>System.currentTimeMillis()){Account existing=accounts.findById(access.accountId).orElse(null);if(existing!=null&&"email_entry".equals(existing.provider)&&email.equals(existing.email))account=existing;}
  }
  // An email address is contact information, never an account lookup credential.
  if(account==null){account=new Account();account.name=email.split("@")[0];account.email=email;account.provider="email_entry";accounts.saveAndFlush(account);String token=SupabaseAuthProvider.randomToken();DeviceAccess access=new DeviceAccess();access.tokenHash=SupabaseAuthProvider.hash(token);access.accountId=account.id;access.expiresAt=System.currentTimeMillis()+31536000000L;devices.saveAndFlush(access);res.addHeader("Set-Cookie",org.springframework.http.ResponseCookie.from("bb_device",token).httpOnly(true).secure(req.isSecure()).sameSite("Lax").path("/").maxAge(31536000).build().toString());}
  establish(account,req,res,86400);return Map.of("success",true,"emailVerified",false);
 }
 @PostMapping("/auth/admin") public Map<String,Boolean> admin(@Valid @RequestBody AdminEntry body,HttpServletRequest req,HttpServletResponse res){
  limits.check("admin-ip:"+req.getRemoteAddr(),8,900000);limits.check("admin-global",60,900000);
  if(!adminCredentials.accepts(body.username(),body.password()))throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid admin ID or password.");
  String external="admin:"+adminCredentials.username;Account a=accounts.findByExternalId(external).orElseGet(Account::new);a.externalId=external;a.name="BloodBridge Admin";a.provider="admin";a.email=null;accounts.saveAndFlush(a);establish(a,req,res,3600);req.getSession().setMaxInactiveInterval(900);return Map.of("success",true);
 }
 public record PhoneStart(@NotBlank @Pattern(regexp="\\+91[6-9][0-9]{9}") String phone,@NotNull @AssertTrue Boolean consent){}
 public record PhoneVerify(@NotBlank @Pattern(regexp="\\+91[6-9][0-9]{9}") String phone,@NotBlank @Pattern(regexp="[0-9]{6}") String code){}
 @GetMapping("/session") public Map<String,Object> session(Authentication auth,HttpServletRequest request,HttpServletResponse response){
  response.setHeader("Cache-Control","private, no-store");Map<String,Object> result=new HashMap<>();result.put("mode","java");result.put("user",null);result.put("profile",null);result.put("authReady",true);result.put("adminReady",adminCredentials.configured());result.put("isAdmin",false);
  CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());result.put("csrf",csrf.getToken());
  if(auth!=null){Account a=accounts.findById(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again."));result.put("user",Map.of("id",a.id,"name",a.name,"email",a.email==null?"":a.email,"phone",a.phone,"emailVerified",!"email_entry".equals(a.provider)));result.put("profile",donors.findByAccountId(a.id).map(DonorController::privateDto).orElse(null));result.put("isAdmin",program.isAdmin(a));}return result;
 }
 @GetMapping("/auth/google") public void google(HttpServletRequest req,HttpServletResponse res) throws IOException {String verifier=SupabaseAuthProvider.randomToken(),url=provider.authorizeUrl(verifier);HttpSession s=req.getSession(true);s.setAttribute("bb_pkce",verifier);s.setAttribute("bb_pkce_expiry",System.currentTimeMillis()+600000);res.setHeader("Cache-Control","no-store");res.sendRedirect(url);}
 @GetMapping("/auth/callback") public void callback(@RequestParam(required=false) String code,HttpServletRequest req,HttpServletResponse res) throws IOException {
  boolean success=false;try{HttpSession s=req.getSession(false);if(s==null)throw new IllegalStateException();Object verifier=s.getAttribute("bb_pkce"),expiry=s.getAttribute("bb_pkce_expiry");s.removeAttribute("bb_pkce");s.removeAttribute("bb_pkce_expiry");if(!(verifier instanceof String v)||!v.matches("[a-zA-Z0-9_-]{43}")||!(expiry instanceof Long exp)||exp<System.currentTimeMillis()||code==null||code.length()>2000)throw new IllegalStateException();signIn(provider.verifyGoogle(code,v),req,res);success=true;}catch(Exception ignored){/* Never expose auth codes or provider responses. */}res.setHeader("Cache-Control","no-store");res.setStatus(303);res.setHeader("Location",success?"/#profile":"/?auth_error=1#home");
 }
 @PostMapping("/auth/phone/start") public Map<String,Boolean> start(@Valid @RequestBody PhoneStart body,HttpServletRequest req){String key=SupabaseAuthProvider.hash(body.phone());limits.check("send:"+key,1,60000);limits.check("sendhour:"+key,5,3600000);limits.check("sendip:"+req.getRemoteAddr(),20,3600000);provider.sendPhone(body.phone());return Map.of("success",true);}
 @PostMapping("/auth/phone/verify") public Map<String,Boolean> verify(@Valid @RequestBody PhoneVerify body,HttpServletRequest req,HttpServletResponse res){limits.check("verify:"+SupabaseAuthProvider.hash(body.phone()),10,600000);signIn(provider.verifyPhone(body.phone(),body.code()),req,res);return Map.of("success",true);}
 @PostMapping("/auth/logout") public Map<String,Boolean> logout(HttpServletRequest req,HttpServletResponse res){HttpSession s=req.getSession(false);if(s!=null)s.invalidate();SecurityContextHolder.clearContext();Cookie cookie=new Cookie("JSESSIONID","");cookie.setPath("/");cookie.setHttpOnly(true);cookie.setMaxAge(0);cookie.setSecure(req.isSecure());res.addCookie(cookie);return Map.of("success",true);}
 private void signIn(SupabaseAuthProvider.Identity identity,HttpServletRequest req,HttpServletResponse res){
  Account a=accounts.findByExternalId(identity.id()).orElseGet(Account::new);a.externalId=identity.id();a.name=identity.name();a.email=identity.email().isBlank()?null:identity.email();a.phone=identity.phone();a.provider=identity.provider();a.passwordHash=null;accounts.saveAndFlush(a);
  establish(a,req,res,identity.expiresIn());
 }
 private void establish(Account a,HttpServletRequest req,HttpServletResponse res,int seconds){Authentication auth=UsernamePasswordAuthenticationToken.authenticated(a.id,null,List.of(new SimpleGrantedAuthority(program.isAdmin(a)?"ROLE_ADMIN":"ROLE_USER")));sessions.onAuthentication(auth,req,res);var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);req.getSession().setAttribute("bb_session_expiry",System.currentTimeMillis()+seconds*1000L);}
}
