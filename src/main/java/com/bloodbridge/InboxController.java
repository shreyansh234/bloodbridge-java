package com.bloodbridge;

import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import static com.bloodbridge.AccessService.fail;

@RestController @RequestMapping("/api")
public class InboxController {
 private final InboxThreadRepository threads;private final InboxMessageRepository messages;private final InboxService inbox;private final AccessService access;
 public InboxController(InboxThreadRepository threads,InboxMessageRepository messages,InboxService inbox,AccessService access){this.threads=threads;this.messages=messages;this.inbox=inbox;this.access=access;}
 public record SendInput(@NotBlank @Size(min=5,max=4000) String body,@NotNull @Min(1) Integer revision){}
 public record ReplyInput(@NotBlank @Size(min=5,max=2000) String body,@NotBlank @Size(max=36) String clientId){}
 public record StartInput(@NotBlank @Size(min=5,max=180) String subject,@NotBlank @Size(min=5,max=2000) String body,@NotBlank @Size(max=36) String clientId){}
 public record ReadInput(@Min(0) long seenAt){}
 private Map<String,Object> threadDto(InboxThread t,boolean admin){Map<String,Object> d=new HashMap<>();d.put("id",t.id);d.put("kind",t.kind);d.put("subject",t.subject);d.put("unread",admin?t.adminUnread:t.memberUnread);d.put("updatedAt",t.updatedAt);d.put("createdAt",t.createdAt);if(admin){d.put("draftText",t.draftText);d.put("draftRevision",t.draftRevision);d.put("sentRevision",t.sentRevision);}return d;}
 private Map<String,Object> list(Account a,boolean admin,String kind,boolean unread,int page){
  if(!Set.of("all","family","donation","profile","message").contains(kind)||page<1||page>100000)throw fail(400,"Invalid inbox filters.");
  Specification<InboxThread> spec=(r,q,c)->admin?c.conjunction():c.equal(r.get("ownerId"),a.id);
  if(!kind.equals("all"))spec=spec.and((r,q,c)->c.equal(r.get("kind"),kind));if(unread)spec=spec.and((r,q,c)->c.isTrue(r.get(admin?"adminUnread":"memberUnread")));
  Page<InboxThread> rows=threads.findAll(spec,PageRequest.of(page-1,20,Sort.by(Sort.Order.desc("updatedAt"),Sort.Order.asc("id"))));
  return Map.of("threads",rows.stream().map(t->threadDto(t,admin)).toList(),"page",page,"pages",Math.max(1,rows.getTotalPages()),"total",rows.getTotalElements(),"unread",admin?threads.countByAdminUnreadTrue():threads.countByOwnerIdAndMemberUnreadTrue(a.id));
 }
 private Map<String,Object> detail(String id,Account a,boolean admin,int page){
  if(page<1||page>100000)throw fail(400,"Invalid message page.");InboxThread t=inbox.owned(id,a,admin,false);
  Page<InboxMessage> result=messages.findByThreadId(id,PageRequest.of(page-1,100,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id"))));
  List<Map<String,Object>> rows=new ArrayList<>(result.stream().map(m->Map.<String,Object>of("id",m.id,"role",m.senderRole,"body",m.body,"createdAt",m.createdAt)).toList());Collections.reverse(rows);
  return Map.of("thread",threadDto(t,admin),"messages",rows,"page",page,"pages",Math.max(1,result.getTotalPages()));
 }
 @GetMapping("/inbox") public Map<String,Object> memberList(Authentication a,@RequestParam(defaultValue="all") String kind,@RequestParam(defaultValue="false") boolean unread,@RequestParam(defaultValue="1") int page){return list(access.account(a),false,kind,unread,page);}
 @GetMapping("/admin/inbox") public Map<String,Object> adminList(Authentication a,@RequestParam(defaultValue="all") String kind,@RequestParam(defaultValue="false") boolean unread,@RequestParam(defaultValue="1") int page){return list(access.admin(a),true,kind,unread,page);}
 @GetMapping("/inbox/unread") public Map<String,Long> memberUnread(Authentication a){return Map.of("unread",threads.countByOwnerIdAndMemberUnreadTrue(access.account(a).id));}
 @GetMapping("/admin/inbox/unread") public Map<String,Long> adminUnread(Authentication a){access.admin(a);return Map.of("unread",threads.countByAdminUnreadTrue());}
 @GetMapping("/inbox/{id}") public Map<String,Object> memberDetail(@PathVariable String id,Authentication a,@RequestParam(defaultValue="1") int page){return detail(id,access.account(a),false,page);}
 @GetMapping("/admin/inbox/{id}") public Map<String,Object> adminDetail(@PathVariable String id,Authentication a,@RequestParam(defaultValue="1") int page){return detail(id,access.admin(a),true,page);}
 @PostMapping("/inbox/{id}/read") public Map<String,Boolean> memberRead(@PathVariable String id,Authentication a,@Valid @RequestBody ReadInput b){inbox.read(id,access.account(a),false,b.seenAt());return Map.of("success",true);}
 @PostMapping("/admin/inbox/{id}/read") public Map<String,Boolean> adminRead(@PathVariable String id,Authentication a,@Valid @RequestBody ReadInput b){inbox.read(id,access.admin(a),true,b.seenAt());return Map.of("success",true);}
 @PostMapping("/admin/inbox/{id}/send") public Map<String,Object> send(@PathVariable String id,Authentication a,@Valid @RequestBody SendInput b){return inbox.send(id,access.admin(a),b.body(),b.revision());}
 @PostMapping("/inbox/{id}/messages") public Map<String,Object> reply(@PathVariable String id,Authentication a,@Valid @RequestBody ReplyInput b){return inbox.reply(id,access.account(a),b.body(),b.clientId());}
 @PostMapping("/inbox") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) public Map<String,Object> start(Authentication a,@Valid @RequestBody StartInput b){return inbox.start(access.account(a),b.subject(),b.body(),b.clientId());}
}
