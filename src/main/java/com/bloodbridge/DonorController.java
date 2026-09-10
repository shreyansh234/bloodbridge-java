package com.bloodbridge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api")
public class DonorController {
 private static final Set<String> GROUPS=Set.of("A+","A-","B+","B-","AB+","AB-","O+","O-");
 private final DonorRepository donors;private final AccountRepository accounts;private final RateLimiter limits;
 public DonorController(DonorRepository d,AccountRepository a,RateLimiter l){donors=d;accounts=a;limits=l;}
 public record ProfileInput(@NotBlank @Size(min=2,max=80) String name,@NotBlank String bloodGroup,@NotBlank @Size(min=2,max=80) String city,@Size(max=80) String area,@NotBlank @Size(max=25) String phone,@NotNull Boolean available,@NotNull @AssertTrue Boolean consent){}
 public record Availability(@NotNull Boolean available){}
 public static Map<String,Object> publicDto(Donor d){return Map.of("id",d.id,"name",d.name,"bloodGroup",d.bloodGroup,"city",d.city,"area",d.area,"available",d.available,"updatedAt",d.updatedAt);}
 public static Map<String,Object> privateDto(Donor d){Map<String,Object> dto=new HashMap<>(publicDto(d));dto.put("phone",d.phone);dto.put("consent",d.consent);return dto;}
 @GetMapping("/donors") public Map<String,Object> search(@RequestParam(defaultValue="all") String group,@RequestParam(defaultValue="") String city,@RequestParam(defaultValue="true") boolean available,@RequestParam(defaultValue="1") int page){
  if(!group.equals("all")&&!GROUPS.contains(group))throw bad("Choose a valid blood group.");if(city.length()>80||page<1||page>100000)throw bad("Invalid search parameters.");
  String location=city.trim().toLowerCase(Locale.ROOT);Specification<Donor> spec=(root,q,cb)->cb.isTrue(root.get("consent"));
  if(!group.equals("all"))spec=spec.and((root,q,cb)->cb.equal(root.get("bloodGroup"),group));
  if(available)spec=spec.and((root,q,cb)->cb.isTrue(root.get("available")));
  if(!location.isBlank()){String term="%"+location.replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";spec=spec.and((root,q,cb)->cb.or(cb.like(cb.lower(root.get("city")),term,'\\'),cb.like(cb.lower(root.get("area")),term,'\\')));}
  long total=donors.count(spec);int pages=Math.max(1,(int)Math.ceil(total/8.0)),current=Math.min(page,pages);
  var results=donors.findAll(spec,PageRequest.of(current-1,8,Sort.by(Sort.Order.desc("available"),Sort.Order.desc("updatedAt"),Sort.Order.asc("id"))));
  return Map.of("donors",results.getContent().stream().map(DonorController::publicDto).toList(),"total",total,"page",current,"pages",pages,"stats",Map.of("registered",donors.countByConsentTrue(),"available",donors.countByConsentTrueAndAvailableTrue(),"cities",donors.countCities()));
 }
 @GetMapping("/donors/{id}/contact") public Map<String,String> contact(@PathVariable String id,Authentication auth){Account a=account(auth);limits.check("contact:"+a.id,30,600000);Donor d=donors.findByIdAndAvailableTrueAndConsentTrue(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"This donor is no longer available to be contacted."));return Map.of("phone",d.phone);}
 @GetMapping("/profile") public Map<String,Object> profile(Authentication auth){Map<String,Object> result=new HashMap<>();result.put("profile",donors.findByAccountId(account(auth).id).map(DonorController::privateDto).orElse(null));return result;}
 @PutMapping("/profile") @Transactional public Map<String,Object> save(@Valid @RequestBody ProfileInput body,Authentication auth){
  if(!GROUPS.contains(body.bloodGroup()))throw bad("Choose a valid blood group.");String phone=body.phone().replaceAll("[\\s()-]","").replaceFirst("^\\+91","");
  if(!phone.matches("[6-9][0-9]{9}"))throw bad("Enter a valid 10-digit Indian mobile number.");
  if(body.name().trim().length()<2||body.city().trim().length()<2)throw bad("Enter your name and city.");
  Account a=account(auth);Donor d=donors.findByAccountId(a.id).orElseGet(Donor::new);d.account=a;d.name=body.name().trim();d.bloodGroup=body.bloodGroup();d.city=body.city().trim();d.area=body.area()==null?"":body.area().trim();d.phone=phone;d.available=body.available();d.consent=true;d.updatedAt=System.currentTimeMillis();donors.saveAndFlush(d);return Map.of("profile",privateDto(d));
 }
 @PatchMapping("/profile/availability") @Transactional public Map<String,Boolean> availability(@Valid @RequestBody Availability body,Authentication auth){Donor d=donors.findByAccountId(account(auth).id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Create a donor profile first."));d.available=body.available();d.updatedAt=System.currentTimeMillis();donors.save(d);return Map.of("success",true);}
 @DeleteMapping("/profile") @Transactional public Map<String,Boolean> delete(Authentication auth){donors.findByAccountId(account(auth).id).ifPresent(donors::delete);return Map.of("success",true);}
 private Account account(Authentication auth){if(auth==null)throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in to continue.");return accounts.findByEmail(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sign in again."));}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
