package com.bloodbridge;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.bloodbridge.AccessService.fail;
@RestController @RequestMapping("/api")
public class DonorController {
 static final Set<String> GROUPS=Set.of("A+","A-","B+","B-","AB+","AB-","O+","O-");
 private final DonorRepository donors;private final AccountRepository accounts;private final AccessService access;private final InboxService inbox;
 public DonorController(DonorRepository donors,AccountRepository accounts,AccessService access,InboxService inbox){this.donors=donors;this.accounts=accounts;this.access=access;this.inbox=inbox;}
 public record ProfileInput(@NotBlank @Size(min=2,max=80) String name,@NotBlank String bloodGroup,@NotBlank @Size(min=2,max=80) String state,@NotBlank @Size(min=2,max=80) String city,@Size(max=80) String area,@NotBlank @Size(max=25) String phone,@NotNull Boolean available,@NotNull @AssertTrue Boolean consent){}
 public record Availability(@NotNull Boolean available){}
 public static Map<String,Object> publicDto(Donor d){return Map.of("id",d.id,"name","Community donor","bloodGroup",d.bloodGroup,"state",d.state,"city",d.city,"area",d.area,"available",d.available,"updatedAt",d.updatedAt);}
 public static Map<String,Object> privateDto(Donor d){Map<String,Object> dto=new HashMap<>(publicDto(d));dto.put("name",d.name);dto.put("email",d.email);dto.put("phone",d.phone);dto.put("consent",d.consent);return dto;}
 static Specification<Donor> contains(String field,String value){String term="%"+value.trim().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%";return (r,q,c)->c.like(c.lower(r.get(field)),term,'\\');}
 @GetMapping("/donors") public Map<String,Object> search(@RequestParam(defaultValue="all") String group,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="") String city,@RequestParam(defaultValue="true") boolean available,@RequestParam(defaultValue="1") int page){
  if(!group.equals("all")&&!GROUPS.contains(group))throw fail(400,"Choose a valid blood group.");if(state.length()>80||city.length()>80||page<1||page>100000)throw fail(400,"Invalid search parameters.");
  Specification<Donor> spec=(r,q,c)->c.isTrue(r.get("consent"));if(!group.equals("all"))spec=spec.and((r,q,c)->c.equal(r.get("bloodGroup"),group));if(available)spec=spec.and((r,q,c)->c.isTrue(r.get("available")));
  if(!state.isBlank())spec=spec.and(contains("state",state));if(!city.isBlank())spec=spec.and(contains("city",city).or(contains("area",city)));
  long total=donors.count(spec);int pages=Math.max(1,(int)Math.ceil(total/8.0)),current=Math.min(page,pages);var results=donors.findAll(spec,PageRequest.of(current-1,8,Sort.by(Sort.Order.desc("available"),Sort.Order.desc("updatedAt"),Sort.Order.asc("id"))));
  return Map.of("donors",results.stream().map(DonorController::publicDto).toList(),"total",total,"page",current,"pages",pages,"stats",Map.of("registered",donors.countByConsentTrue(),"available",donors.countByConsentTrueAndAvailableTrue(),"cities",donors.countCities()));
 }
 @GetMapping("/admin/donors") public Map<String,Object> adminSearch(Authentication auth,@RequestParam(defaultValue="") String state,@RequestParam(defaultValue="") String city,@RequestParam(defaultValue="") String email,@RequestParam(defaultValue="") String phone,@RequestParam(defaultValue="") String name,@RequestParam(defaultValue="all") String group,@RequestParam(defaultValue="1") int page){
  access.admin(auth);if(page<1||page>100000||state.length()>80||city.length()>80||email.length()>180||phone.length()>25||name.length()>80||(!group.equals("all")&&!GROUPS.contains(group)))throw fail(400,"Invalid filters.");
  Specification<Donor> spec=(r,q,c)->c.conjunction();String[] keys={"state","city","email","phone","name"},values={state,city,email,phone.replaceAll("[\\s()-]","").replaceFirst("^\\+91",""),name};for(int i=0;i<keys.length;i++)if(!values[i].isBlank())spec=spec.and(contains(keys[i],values[i]));if(!group.equals("all"))spec=spec.and((r,q,c)->c.equal(r.get("bloodGroup"),group));
  var result=donors.findAll(spec,PageRequest.of(page-1,20,Sort.by(Sort.Order.desc("updatedAt"),Sort.Order.asc("id"))));return Map.of("donors",result.stream().map(DonorController::privateDto).toList(),"total",result.getTotalElements(),"pages",Math.max(1,result.getTotalPages()),"page",page);
 }
 @GetMapping("/donors/{id}/contact") public Map<String,String> contact(@PathVariable String id,Authentication auth){access.admin(auth);Donor d=donors.findById(id).orElseThrow(()->fail(404,"Donor not found."));return Map.of("phone",d.phone);}
 @GetMapping("/profile") public Map<String,Object> profile(Authentication auth){Map<String,Object> result=new HashMap<>();result.put("profile",donors.findByAccountId(access.account(auth).id).map(DonorController::privateDto).orElse(null));return result;}
 @PutMapping("/profile") @Transactional public Map<String,Object> save(@Valid @RequestBody ProfileInput b,Authentication auth){
  if(!GROUPS.contains(b.bloodGroup()))throw fail(400,"Choose a valid blood group.");String phone=b.phone().replaceAll("[\\s()-]","").replaceFirst("^\\+91","");if(!phone.matches("[6-9][0-9]{9}"))throw fail(400,"Enter a valid Indian mobile number.");if(b.name().trim().length()<2||b.state().trim().length()<2||b.city().trim().length()<2)throw fail(400,"Enter your name, state and city.");
  Account a=access.account(auth);a=accounts.lockById(a.id).orElseThrow();Donor d=donors.findByAccountId(a.id).orElseGet(Donor::new);d.account=a;d.name=b.name().trim();d.email=a.email==null?"":a.email;d.bloodGroup=b.bloodGroup();d.state=b.state().trim();d.city=b.city().trim();d.area=b.area()==null?"":b.area().trim();d.phone=phone;d.available=b.available();d.consent=true;d.updatedAt=System.currentTimeMillis();a.name=d.name;a.phone=phone;accounts.save(a);donors.saveAndFlush(d);inbox.profile(d,"saved");return Map.of("profile",privateDto(d));
 }
 @PatchMapping("/profile/availability") @Transactional public Map<String,Boolean> availability(@Valid @RequestBody Availability b,Authentication auth){Donor d=donors.findByAccountId(access.account(auth).id).orElseThrow(()->fail(404,"Create your profile first."));d.available=b.available();d.updatedAt=System.currentTimeMillis();donors.save(d);inbox.profile(d,"updated");return Map.of("success",true);}
 @DeleteMapping("/profile") @Transactional public Map<String,Boolean> delete(Authentication auth){donors.findByAccountId(access.account(auth).id).ifPresent(d->{inbox.profile(d,"removed");donors.delete(d);});return Map.of("success",true);}
}
