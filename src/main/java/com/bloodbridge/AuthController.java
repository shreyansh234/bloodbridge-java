package com.bloodbridge;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api")
public class AuthController {
 private final AccountRepository accounts;private final DonorRepository donors;private final PasswordEncoder encoder;private final AuthenticationManager manager;private final SecurityContextRepository contexts;private final SessionAuthenticationStrategy sessions;private final RateLimiter limits;
 public AuthController(AccountRepository a,DonorRepository d,PasswordEncoder e,AuthenticationManager m,SecurityContextRepository c,SessionAuthenticationStrategy s,RateLimiter l){accounts=a;donors=d;encoder=e;manager=m;contexts=c;sessions=s;limits=l;}
 public record Register(@NotBlank @Size(min=2,max=80) String name,@NotBlank @Email @Size(max=180) String email,@NotBlank @Size(min=10,max=72) String password){}
 public record Login(@NotBlank @Email @Size(max=180) String email,@NotBlank @Size(max=72) String password){}
 @GetMapping("/session") public Map<String,Object> session(Authentication auth,HttpServletRequest request,HttpServletResponse response) {
  response.setHeader("Cache-Control","no-store");
  Map<String,Object> result=new HashMap<>();result.put("mode","java");result.put("user",null);result.put("profile",null);
  CsrfToken csrf=(CsrfToken)request.getAttribute(CsrfToken.class.getName());result.put("csrf",csrf.getToken());
  if(auth!=null){Account a=accounts.findByEmail(auth.getName()).orElseThrow();result.put("user",Map.of("id",a.id,"name",a.name,"email",a.email));result.put("profile",donors.findByAccountId(a.id).map(DonorController::privateDto).orElse(null));}
  return result;
 }
 @PostMapping("/auth/register") @ResponseStatus(HttpStatus.CREATED) public Map<String,Boolean> register(@Valid @RequestBody Register body,HttpServletRequest req,HttpServletResponse res) {
  limits.check("register:"+req.getRemoteAddr(),5,900000);String email=body.email().trim().toLowerCase(Locale.ROOT),name=body.name().trim();
  if(name.length()<2)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enter your full name.");
  checkPasswordBytes(body.password());
  if(accounts.findByEmail(email).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists.");
  accounts.saveAndFlush(new Account(name,email,encoder.encode(body.password())));signIn(email,body.password(),req,res);return Map.of("success",true);
 }
 @PostMapping("/auth/login") public Map<String,Boolean> login(@Valid @RequestBody Login body,HttpServletRequest req,HttpServletResponse res) {
  String email=body.email().trim().toLowerCase(Locale.ROOT);limits.check("login-ip:"+req.getRemoteAddr(),30,900000);limits.check("login-account:"+email,15,900000);checkPasswordBytes(body.password());signIn(email,body.password(),req,res);return Map.of("success",true);
 }
 @PostMapping("/auth/logout") public Map<String,Boolean> logout(HttpServletRequest req,HttpServletResponse res) {
  HttpSession session=req.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();
  jakarta.servlet.http.Cookie cookie=new jakarta.servlet.http.Cookie("JSESSIONID","");cookie.setPath("/");cookie.setHttpOnly(true);cookie.setMaxAge(0);cookie.setSecure(req.isSecure());res.addCookie(cookie);return Map.of("success",true);
 }
 private void signIn(String email,String password,HttpServletRequest req,HttpServletResponse res) {
  Authentication auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email,password));sessions.onAuthentication(auth,req,res);
  var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);
 }
 private void checkPasswordBytes(String password){if(password.getBytes(StandardCharsets.UTF_8).length>72)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Password must be at most 72 UTF-8 bytes.");}
}
