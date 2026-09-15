package com.bloodbridge;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bloodbridge.AccessService.fail;

@Service
public class InboxService {
 private final InboxThreadRepository threads;
 private final InboxMessageRepository messages;
 private final AccountRepository accounts;
 private final RateLimiter limits;
 public InboxService(InboxThreadRepository threads,InboxMessageRepository messages,AccountRepository accounts,RateLimiter limits){this.threads=threads;this.messages=messages;this.accounts=accounts;this.limits=limits;}

 private void touch(InboxThread t){t.updatedAt=Math.max(System.currentTimeMillis(),t.updatedAt+1);}
 private void append(InboxThread t,String role,String sender,String body,String key){
  InboxMessage m=new InboxMessage();m.threadId=t.id;m.senderRole=role;m.senderId=sender;m.body=body;m.clientKey=key;m.createdAt=t.updatedAt;messages.saveAndFlush(m);
 }
 @Transactional
 public InboxThread record(String owner,String kind,String source,String subject,String body,String draft){
  String key=kind+":"+source+":"+owner;
  InboxThread t=threads.lockBySourceKey(key).orElseGet(()->{InboxThread n=new InboxThread();n.ownerId=owner;n.kind=kind;n.sourceKey=key;return n;});
  t.subject=subject;t.draftText=draft;t.draftRevision++;t.adminUnread=true;t.memberUnread=true;touch(t);threads.saveAndFlush(t);
  append(t,"system","",body,"event-"+UUID.randomUUID());return t;
 }
 @Transactional
 public void profile(Donor d,String action){
  String body="Donor profile "+action+".\nName: "+d.name+"\nBlood group: "+d.bloodGroup+"\nLocation: "+d.city+", "+d.state+"\nAvailability: "+(d.available?"available":"paused");
  record(d.account.getId(),"profile",d.account.getId(),d.name+" · donor profile",body,"Hi "+d.name+", your donor profile has been "+action+". Keep your blood group, location and availability up to date. Both people must approve a blood request before phone numbers are shared. — BloodBridge team");
 }
 @Transactional
 public void family(FamilyRequest f){
  String body=String.join("\n","Family support request: "+f.status.replace('_',' '),"Request ID: "+f.id,"Saviour: "+f.requesterName,"Email (unverified): "+f.email,"Patient: "+f.patientName,"Relationship: "+f.relationship,"Required blood group: "+f.bloodGroup,"Hospital: "+f.hospital,"State: "+f.state,"City: "+f.city,"Phone: +91 "+f.phone,f.note.isBlank()?"":("Coordination update: "+f.note));
  String update=switch(f.status){case "closed"->"The administrator has marked this coordination request completed.";case "cancelled"->"This request has been cancelled and one family-support use is available again.";case "in_progress"->"Your request is now being coordinated.";default->"We have received your family-support request.";};
  String draft="Hi "+f.requesterName+", "+update+" Required blood group: "+f.bloodGroup+" for "+f.patientName+" at "+f.hospital+", "+f.city+". "+(f.note.isBlank()?"":f.note+" ")+"Please keep your contact number available and continue working with the treating hospital. Blood availability requires hospital confirmation. Reply here if any details change. — BloodBridge team";
  record(f.ownerId,"family",f.id,"Family support · "+f.bloodGroup+" · "+f.city,body,draft);
 }
 @Transactional
 public void match(BloodMatch m,String event){
  for(String owner:List.of(m.donorId,m.receiverId)){
   boolean donor=owner.equals(m.donorId);
   String body=String.join("\n",event,"Request ID: "+m.id,"Patient: "+m.patientName,"Blood group: "+m.bloodGroup,"Hospital: "+m.hospital,"Location: "+m.city+", "+m.state,"Status: "+m.status,"Donor approval: "+(m.donorApproved?"approved":"pending"),"Receiver approval: "+(m.receiverApproved?"approved":"pending"));
   String next=switch(m.status){case "accepted"->"Both people have approved. Open Requests to view the shared contact details and coordinate through the hospital. Only the receiver can mark blood received.";case "received"->donor?"The receiver confirmed blood received. Your completed donation and 500 recognition points have been recorded.":"Your blood received confirmation has been saved to your profile. Thank you for updating the donor.";case "declined"->"This request was declined. Contact sharing remains closed. You can use Find donors to start a new request.";default->"Open Requests to approve or decline. Contact details become available only after both people approve.";};
   record(owner,"donation",m.id,(donor?"Donation request":"Blood request")+" · "+m.bloodGroup+" · "+m.city,body,"Hello, here is an update on your "+m.bloodGroup+" blood request at "+m.hospital+". "+next+" — BloodBridge team");
  }
 }
 public InboxThread owned(String id,Account actor,boolean admin,boolean lock){
  InboxThread t=(lock?threads.lockById(id):threads.findById(id)).orElseThrow(()->fail(404,"Conversation not found."));
  if(!admin&&!t.ownerId.equals(actor.id))throw fail(404,"Conversation not found.");return t;
 }
 @Transactional
 public void read(String id,Account actor,boolean admin,long seenAt){InboxThread t=owned(id,actor,admin,true);if(seenAt>=t.updatedAt){if(admin)t.adminUnread=false;else t.memberUnread=false;threads.save(t);}}
 @Transactional
 public Map<String,Object> send(String id,Account admin,String body,int revision){
  InboxThread t=owned(id,admin,true,true);
  if(revision==t.sentRevision)return Map.of("success",true,"alreadySent",true);
  if(revision!=t.draftRevision||t.draftText.isBlank())throw fail(409,"A newer update is available. Refresh the conversation before sending.");
  limits.check("inbox-send:"+admin.id,120,3600000);touch(t);append(t,"admin",admin.id,clean(body,4000),"draft-"+revision);
  t.sentRevision=revision;t.draftText="";t.adminUnread=false;t.memberUnread=true;threads.save(t);return Map.of("success",true,"alreadySent",false);
 }
 @Transactional
 public Map<String,Object> reply(String id,Account actor,String body,String clientId){
  InboxThread t=owned(id,actor,false,true);String key=uuid(clientId);
  if(messages.existsByThreadIdAndClientKey(id,key))return Map.of("success",true,"alreadySent",true);
  limits.check("inbox-reply:"+actor.id,30,3600000);touch(t);append(t,"member",actor.id,clean(body,2000),key);
  t.adminUnread=true;t.memberUnread=false;t.draftRevision++;t.draftText="Hi "+actor.name+", thank you for the update about “"+t.subject+"”. We have received your message. Please share any changes to the hospital, required blood group or contact details in this conversation. — BloodBridge team";threads.save(t);return Map.of("success",true,"alreadySent",false);
 }
 @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public Map<String,Object> start(Account actor,String subject,String body,String clientId){
  accounts.lockById(actor.id).orElseThrow();String source=uuid(clientId),key="message:"+source+":"+actor.id;
  Optional<InboxThread> existing=threads.findBySourceKey(key);if(existing.isPresent())return Map.of("id",existing.get().id,"success",true);
  limits.check("inbox-new:"+actor.id,10,86400000);
  InboxThread t=new InboxThread();t.ownerId=actor.id;t.sourceKey=key;t.kind="message";t.subject=clean(subject,180);t.draftRevision=1;t.memberUnread=false;t.draftText="Hi "+actor.name+", thank you for contacting BloodBridge about “"+t.subject+"”. We have received your message. You can follow this conversation for updates from our team. — BloodBridge team";threads.saveAndFlush(t);append(t,"member",actor.id,clean(body,2000),source);return Map.of("id",t.id,"success",true);
 }
 private String clean(String text,int max){String v=text==null?"":text.trim();if(v.length()<5||v.length()>max)throw fail(400,"Enter between 5 and "+max+" characters.");return v;}
 private String uuid(String value){try{return UUID.fromString(value).toString();}catch(Exception e){throw fail(400,"Refresh the page and try sending again.");}}
}
