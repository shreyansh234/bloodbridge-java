package com.bloodbridge;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.event.*;
@Service
public class FamilyEmailService {
 public static final String RECIPIENT="bloodbridgeadmin@gmail.com";
 private final EmailNoticeRepository notices;private final TransactionTemplate tx;private final ObjectMapper json;private final String key,from;
 private final EmailGateway gateway;
 public FamilyEmailService(EmailGateway gateway,EmailNoticeRepository notices,org.springframework.transaction.PlatformTransactionManager manager,ObjectMapper json,@Value("${bloodbridge.resend-api-key:}") String key,@Value("${bloodbridge.mail-from:}") String from){this.gateway=gateway;this.notices=notices;this.tx=new TransactionTemplate(manager);this.tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);this.json=json;this.key=key;this.from=from;}
 public boolean configured(){return !key.isBlank()&&!from.isBlank();}
 @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT) public void created(FamilyNoticeCreated event){send(event.id());}
 @Scheduled(fixedDelayString="${bloodbridge.mail-retry-ms:60000}",initialDelayString="${bloodbridge.mail-initial-delay-ms:60000}") public void retry(){if(configured())for(EmailNotice n:notices.findTop20ByStatusInAndAttemptsLessThanAndNextAttemptLessThanEqualOrderByNextAttemptAsc(List.of("pending","sending"),5,System.currentTimeMillis()))send(n.id);}
 public void retryNow(String id){
  Boolean ready=tx.execute(status->{
   EmailNotice row=notices.lockById(id).orElseThrow(()->AccessService.fail(404,"Notification not found."));
   if(row.status.equals("sent")||(row.status.equals("sending")&&row.nextAttempt>System.currentTimeMillis()))return false;
   if(row.status.equals("failed"))row.attempts=0;
   row.status="pending";row.nextAttempt=0;notices.saveAndFlush(row);return true;
  });
  if(Boolean.TRUE.equals(ready))send(id);
 }
 public void send(String id){if(!configured())return;
  EmailNotice n=tx.execute(status->{EmailNotice row=notices.lockById(id).orElse(null);if(row==null||row.status.equals("sent")||row.attempts>=5||row.nextAttempt>System.currentTimeMillis())return null;row.status="sending";row.attempts++;row.nextAttempt=System.currentTimeMillis()+60000;notices.saveAndFlush(row);return row;});if(n==null)return;
  boolean sent=false;String providerId="",error="Email provider could not accept the message.";
  try{providerId=gateway.send(key,from,RECIPIENT,n.subject,n.messageBody,"family-"+id);sent=!providerId.isBlank();}catch(Exception ignored){/* Preserve pending delivery without exposing credentials or family information. */}
  final boolean ok=sent;final String ref=providerId,reason=error;tx.executeWithoutResult(status->{EmailNotice row=notices.lockById(id).orElseThrow();row.status=ok?"sent":"pending";row.providerId=ref;row.lastError=ok?"":reason;row.nextAttempt=System.currentTimeMillis()+Math.min(3600000L,60000L*(1L<<row.attempts));if(!ok&&row.attempts>=5)row.status="failed";notices.save(row);});
 }
}
