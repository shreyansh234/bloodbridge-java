package com.bloodbridge;
import com.fasterxml.jackson.databind.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest(properties={"bloodbridge.family-assistance-enabled=true","bloodbridge.resend-api-key=test-only-key","bloodbridge.mail-from=BloodBridge <test@example.org>","bloodbridge.mail-initial-delay-ms=3600000"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class BloodBridgeFlowTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper json;@Autowired AccountRepository accounts;@Autowired DonorRepository donors;@Autowired DeviceAccessRepository devices;@Autowired DonationRecordRepository donations;@Autowired BloodMatchRepository matches;@Autowired FamilyRequestRepository families;@Autowired EmailNoticeRepository notices;
 @MockitoBean EmailGateway gateway;
 @Autowired FamilyEmailService email;
 @Autowired InboxThreadRepository inboxThreads;
 @Autowired InboxMessageRepository inboxMessages;
 private static final java.util.concurrent.atomic.AtomicInteger adminClient=new java.util.concurrent.atomic.AtomicInteger();
 @DynamicPropertySource static void credentials(DynamicPropertyRegistry p) throws Exception {String salt=Base64.getEncoder().encodeToString(new byte[16]);String key=Base64.getEncoder().encodeToString(AdminCredentials.derive("fixture-pass",new byte[16],210000));p.add("ADMIN_USERNAME",()->"fixture-admin");p.add("ADMIN_PASSWORD_HASH",()->"pbkdf2:210000:"+salt+":"+key);}
 @BeforeEach void resetData() throws Exception {inboxMessages.deleteAll();inboxThreads.deleteAll();notices.deleteAll();families.deleteAll();matches.deleteAll();donations.deleteAll();donors.deleteAll();devices.deleteAll();accounts.deleteAll();accounts.flush();reset(gateway);when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenReturn("provider-test-id");}
 private record Entry(MockHttpSession session,Cookie device){}
 private Entry enter(String address,Cookie... cookies) throws Exception {var builder=post("/api/auth/email").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("email",address)));if(cookies.length>0)builder.cookie(cookies);var r=mvc.perform(builder).andExpect(status().isOk()).andReturn();String header=r.getResponse().getHeader("Set-Cookie");Cookie device=header==null?null:new Cookie("bb_device",header.split(";",2)[0].substring("bb_device=".length()));return new Entry((MockHttpSession)r.getRequest().getSession(),device);}
 private MockHttpSession admin() throws Exception {return (MockHttpSession)mvc.perform(post("/api/auth/admin").with(r->{r.setRemoteAddr("192.0.2."+adminClient.incrementAndGet());return r;}).with(csrf()).contentType("application/json").content("{\"username\":\"fixture-admin\",\"password\":\"fixture-pass\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();}
 private String profile(MockHttpSession s,String name,String state,String city,String phone) throws Exception {var r=mvc.perform(put("/api/profile").session(s).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("name",name,"state",state,"city",city,"area","Central","phone",phone,"bloodGroup","O+","available",true,"consent",true)))).andExpect(status().isOk()).andReturn();return json.readTree(r.getResponse().getContentAsString()).path("profile").path("id").asText();}
 private String request(MockHttpSession s,String donor) throws Exception {var r=mvc.perform(post("/api/matches").session(s).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("donorId",donor,"patientName","Test Patient","hospital","Test Hospital","bloodGroup","O+","state","Maharashtra","city","Thane","note","Coordinate at hospital","consent",true)))).andExpect(status().isCreated()).andReturn();return json.readTree(r.getResponse().getContentAsString()).path("id").asText();}
 private ResultActions approve(MockHttpSession s,String id,boolean approve) throws Exception {return mvc.perform(patch("/api/matches/"+id+"/decision").session(s).with(csrf()).contentType("application/json").content("{\"approve\":"+approve+"}"));}
 private ResultActions received(MockHttpSession s,String id) throws Exception{return mvc.perform(post("/api/matches/"+id+"/received").session(s).with(csrf()).contentType("application/json").content("{\"confirmed\":true,\"donationDate\":\""+LocalDate.now(ZoneOffset.UTC)+"\"}"));}
 @Test void adminEnvironmentEnablesTheExistingPortalAndDiagnosticsAreRemoved() throws Exception {
  mvc.perform(get("/api/session")).andExpect(status().isOk()).andExpect(jsonPath("$.adminReady").value(true)).andExpect(jsonPath("$.isAdmin").value(false));
  mvc.perform(get("/api/diagnostic/admin")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/admin").contentType("application/json").content("{\"username\":\"fixture-admin\",\"password\":\"fixture-pass\"}")).andExpect(status().isForbidden());
  MockHttpSession s=admin();
  mvc.perform(get("/api/session").session(s)).andExpect(status().isOk()).andExpect(jsonPath("$.adminReady").value(true)).andExpect(jsonPath("$.isAdmin").value(true));
  mvc.perform(get("/api/diagnostic/admin").session(s)).andExpect(status().isNotFound());
  mvc.perform(post("/api/auth/logout").session(s).with(csrf())).andExpect(status().isOk());
  mvc.perform(get("/api/session")).andExpect(jsonPath("$.isAdmin").value(false));
 }
 @Test void emailEntryCannotClaimAnotherDeviceAndAdminRecordsStayPrivate() throws Exception {
  mvc.perform(post("/api/auth/email").contentType("application/json").content("{\"email\":\"a@example.org\"}")).andExpect(status().isForbidden());
  Entry alice=enter("alice@example.org");String id=profile(alice.session,"Alice Donor","Maharashtra","Thane","9000000001");
  mvc.perform(get("/api/donors?state=maharashtra&city=thane")).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.donors[0].name").value("Community donor")).andExpect(jsonPath("$.donors[0].email").doesNotExist()).andExpect(jsonPath("$.donors[0].phone").doesNotExist());
  Entry stranger=enter("alice@example.org");mvc.perform(get("/api/session").session(stranger.session)).andExpect(jsonPath("$.profile").isEmpty()).andExpect(jsonPath("$.isAdmin").value(false));
  mvc.perform(get("/api/admin/donors").session(alice.session)).andExpect(status().isForbidden());mvc.perform(get("/api/donors/"+id+"/contact").session(alice.session)).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/admin").with(csrf()).contentType("application/json").content("{\"username\":\"fixture-admin\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
  MockHttpSession admin=admin();mvc.perform(get("/api/admin/donors?state=Maharashtra&city=Thane&email=alice&phone=9000000001").session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.donors[0].name").value("Alice Donor")).andExpect(jsonPath("$.donors[0].email").value("alice@example.org"));
  mvc.perform(get("/api/admin/donors?state=Gujarat").session(admin)).andExpect(jsonPath("$.total").value(0));
  mvc.perform(post("/api/auth/logout").session(alice.session).with(csrf())).andExpect(status().isOk());Entry restored=enter("alice@example.org",alice.device);mvc.perform(get("/api/session").session(restored.session)).andExpect(jsonPath("$.profile.id").value(id));
  assertTrue(devices.findAll().stream().allMatch(d->d.tokenHash.length()==64&&!d.tokenHash.equals(alice.device.getValue())));
 }
 @Test void onlyReceiverAfterMutualApprovalCanCreditExactlyOnce() throws Exception {
  Entry donor=enter("donor@example.org"),receiver=enter("receiver@example.org"),stranger=enter("outsider@example.org");String donorId=profile(donor.session,"Donor Person","Maharashtra","Thane","9000000002");profile(receiver.session,"Receiver Person","Maharashtra","Thane","9000000003");String id=request(receiver.session,donorId);
  received(receiver.session,id).andExpect(status().isConflict());approve(stranger.session,id,true).andExpect(status().isNotFound());mvc.perform(get("/api/matches/"+id+"/contact").session(receiver.session)).andExpect(status().isForbidden());
  approve(donor.session,id,true).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("pending"));mvc.perform(get("/api/matches/"+id+"/contact").session(receiver.session)).andExpect(status().isForbidden());
  approve(receiver.session,id,true).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("accepted"));mvc.perform(get("/api/matches/"+id+"/contact").session(receiver.session)).andExpect(status().isOk()).andExpect(jsonPath("$.phone").value("9000000002")).andExpect(jsonPath("$.email").doesNotExist());
  mvc.perform(get("/api/matches/"+id+"/contact").session(stranger.session)).andExpect(status().isNotFound());received(donor.session,id).andExpect(status().isForbidden());received(receiver.session,id).andExpect(status().isOk()).andExpect(jsonPath("$.pointsAdded").value(500));received(receiver.session,id).andExpect(status().isOk()).andExpect(jsonPath("$.alreadyRecorded").value(true));
  mvc.perform(get("/api/rewards").session(donor.session)).andExpect(jsonPath("$.points").value(500)).andExpect(jsonPath("$.verifiedDonations").value(1));mvc.perform(get("/api/matches").session(receiver.session)).andExpect(jsonPath("$.bloodReceived").value(1)).andExpect(jsonPath("$.matches[0].status").value("received"));
  mvc.perform(post("/api/donations").session(donor.session).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isConflict());mvc.perform(patch("/api/admin/donations/anything").session(admin()).with(csrf()).contentType("application/json").content("{\"status\":\"verified\"}")).andExpect(status().isConflict());
  String next=request(receiver.session,donorId);approve(donor.session,next,true);approve(receiver.session,next,true);received(receiver.session,next).andExpect(status().isConflict());assertEquals(1,donations.count());
 }
 @Test void declinedRequestCannotExposeContactOrBeMarkedReceived() throws Exception {
  Entry donor=enter("decline-donor@example.org"),receiver=enter("decline-receiver@example.org");String d=profile(donor.session,"Test Donor","Maharashtra","Kalyan","9000000004");profile(receiver.session,"Test Receiver","Maharashtra","Kalyan","9000000005");String id=request(receiver.session,d);
  approve(donor.session,id,false).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("declined"));approve(receiver.session,id,true).andExpect(status().isConflict());received(receiver.session,id).andExpect(status().isConflict());mvc.perform(get("/api/matches/"+id+"/contact").session(receiver.session)).andExpect(status().isForbidden());assertEquals(0,donations.count());
 }
 private String familyBody() throws Exception {return json.writeValueAsString(Map.of("relationship","Parent","patientName","Test Family Member","hospital","Test Hospital","state","Maharashtra","city","Kalyan","bloodGroup","O-","phone","9000000006","consent",true));}
 private void seedSaviour(Entry e) throws Exception {String id=json.readTree(mvc.perform(get("/api/session").session(e.session)).andReturn().getResponse().getContentAsString()).path("user").path("id").asText();for(int i=0;i<10;i++){DonationRecord d=new DonationRecord();d.ownerId=id;d.donationDate=LocalDate.of(2020,1,1).plusMonths(i*4).toString();d.center="Fixture centre";d.reference="FIXTURE-"+i;d.referenceKey=SupabaseAuthProvider.hash(d.reference);d.status="verified";donations.saveAndFlush(d);}}
 @Test void saviourHasOnlyTwoAtomicUsesAndEmailContainsTheWholeRequest() throws Exception {
  Entry saviour=enter("saviour@example.org"),other=enter("non-saviour@example.org");profile(saviour.session,"Saviour Person","Maharashtra","Kalyan","9000000006");seedSaviour(saviour);String body=familyBody();
  mvc.perform(post("/api/family").session(other.session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isForbidden());
  ExecutorService pool=Executors.newFixedThreadPool(4);List<Future<Integer>> results=new ArrayList<>();try{for(int i=0;i<4;i++)results.add(pool.submit(()->mvc.perform(post("/api/family").session(saviour.session).with(csrf()).contentType("application/json").content(body)).andReturn().getResponse().getStatus()));List<Integer> codes=new ArrayList<>();for(var f:results)codes.add(f.get(30,TimeUnit.SECONDS));assertEquals(2,Collections.frequency(codes,201));assertEquals(2,Collections.frequency(codes,409));}finally{pool.shutdownNow();}
  assertEquals(2,families.count());assertEquals(2,notices.count());assertTrue(notices.findAll().stream().allMatch(n->n.status.equals("sent")));verify(gateway,times(2)).send(eq("test-only-key"),anyString(),eq("bloodbridgeadmin@gmail.com"),contains("O-"),argThat(text->text.contains("saviour@example.org")&&text.contains("Test Family Member")&&text.contains("Maharashtra")&&text.contains("Kalyan")&&text.contains("9000000006")&&text.contains("Parent")),startsWith("family-"));
  mvc.perform(get("/api/rewards").session(saviour.session)).andExpect(jsonPath("$.saviour").value(true)).andExpect(jsonPath("$.familyUsesRemaining").value(0));String id=families.findAll().get(0).id;
  mvc.perform(patch("/api/admin/family/"+id).session(admin()).with(csrf()).contentType("application/json").content("{\"status\":\"cancelled\",\"note\":\"Patient no longer needs support\"}")).andExpect(status().isOk());mvc.perform(post("/api/family").session(saviour.session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated());
 }
 @Test void deliveryFailurePreservesRequestAndRetryDoesNotSendTwice() throws Exception {
  when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("provider unavailable")).thenReturn("retry-provider-id");Entry e=enter("queued@example.org");profile(e.session,"Queued Saviour","Maharashtra","Thane","9000000007");seedSaviour(e);
  var r=mvc.perform(post("/api/family").session(e.session).with(csrf()).contentType("application/json").content(familyBody())).andExpect(status().isCreated()).andReturn();String id=json.readTree(r.getResponse().getContentAsString()).path("id").asText();assertEquals("pending",notices.findById(id).orElseThrow().status);assertTrue(families.existsById(id));MockHttpSession admin=admin();
  mvc.perform(post("/api/admin/family/"+id+"/email/retry").session(admin).with(csrf())).andExpect(status().isOk());assertEquals("sent",notices.findById(id).orElseThrow().status);mvc.perform(post("/api/admin/family/"+id+"/email/retry").session(admin).with(csrf())).andExpect(status().isOk());verify(gateway,times(2)).send(anyString(),anyString(),anyString(),anyString(),anyString(),eq("family-"+id));
 }
 @Test void administratorCanRecoverAnExhaustedNoticeWithoutDuplicatingDelivery() throws Exception {
  when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("provider unavailable"));
  Entry e=enter("recovery@example.org");profile(e.session,"Recovery Saviour","Maharashtra","Thane","9000000008");seedSaviour(e);
  var r=mvc.perform(post("/api/family").session(e.session).with(csrf()).contentType("application/json").content(familyBody())).andExpect(status().isCreated()).andReturn();String id=json.readTree(r.getResponse().getContentAsString()).path("id").asText();
  for(int i=1;i<5;i++){EmailNotice n=notices.findById(id).orElseThrow();n.nextAttempt=0;notices.saveAndFlush(n);email.send(id);}
  EmailNotice exhausted=notices.findById(id).orElseThrow();assertEquals("failed",exhausted.status);assertEquals(5,exhausted.attempts);exhausted.nextAttempt=0;notices.saveAndFlush(exhausted);
  email.retry();verify(gateway,times(5)).send(anyString(),anyString(),anyString(),anyString(),anyString(),eq("family-"+id));
  doReturn("recovered-provider-id").when(gateway).send(anyString(),anyString(),anyString(),anyString(),anyString(),anyString());MockHttpSession admin=admin();
  mvc.perform(post("/api/admin/family/"+id+"/email/retry").session(e.session).with(csrf())).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/family/"+id+"/email/retry").session(admin).with(csrf())).andExpect(status().isOk());
  assertEquals("sent",notices.findById(id).orElseThrow().status);assertTrue(families.existsById(id));
  mvc.perform(post("/api/admin/family/"+id+"/email/retry").session(admin).with(csrf())).andExpect(status().isOk());
  verify(gateway,times(6)).send(anyString(),anyString(),anyString(),anyString(),anyString(),eq("family-"+id));
 }
 private JsonNode getJson(String path,MockHttpSession s) throws Exception{return json.readTree(mvc.perform(get(path).session(s)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());}
 @Test void familyInboxWorksWhenEmailFailsAndDraftStaysPrivateUntilManualSend() throws Exception {
  when(gateway.send(anyString(),anyString(),anyString(),anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("provider unavailable"));
  Entry owner=enter("inbox-family@example.org"),stranger=enter("inbox-family@example.org");profile(owner.session,"Inbox Saviour","Maharashtra","Kalyan","9000000009");seedSaviour(owner);
  mvc.perform(post("/api/family").session(owner.session).with(csrf()).contentType("application/json").content(familyBody())).andExpect(status().isCreated());
  JsonNode list=getJson("/api/inbox?kind=family",owner.session);assertEquals(1,list.path("total").asInt());String id=list.path("threads").get(0).path("id").asText();assertFalse(list.path("threads").get(0).has("draftText"));
  JsonNode before=getJson("/api/inbox/"+id,owner.session);assertEquals(1,before.path("messages").size());assertEquals("system",before.path("messages").get(0).path("role").asText());assertFalse(before.path("thread").has("draftRevision"));assertEquals(0,inboxMessages.countByThreadIdAndSenderRole(id,"admin"));
  mvc.perform(get("/api/inbox/"+id).session(stranger.session)).andExpect(status().isNotFound());mvc.perform(get("/api/admin/inbox").session(owner.session)).andExpect(status().isForbidden());assertEquals(0,getJson("/api/inbox",stranger.session).path("total").asInt());
  MockHttpSession admin=admin();JsonNode adminDetail=getJson("/api/admin/inbox/"+id,admin);String draft=adminDetail.path("thread").path("draftText").asText();assertTrue(draft.contains("O-")&&draft.contains("Test Family Member"));String send=json.writeValueAsString(Map.of("body",draft,"revision",adminDetail.path("thread").path("draftRevision").asInt()));
  mvc.perform(post("/api/admin/inbox/"+id+"/send").session(owner.session).with(csrf()).contentType("application/json").content(send)).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/inbox/"+id+"/send").session(admin).contentType("application/json").content(send)).andExpect(status().isForbidden());
  mvc.perform(post("/api/admin/inbox/"+id+"/send").session(admin).with(csrf()).contentType("application/json").content(send)).andExpect(status().isOk()).andExpect(jsonPath("$.alreadySent").value(false));
  mvc.perform(post("/api/admin/inbox/"+id+"/send").session(admin).with(csrf()).contentType("application/json").content(send)).andExpect(status().isOk()).andExpect(jsonPath("$.alreadySent").value(true));assertEquals(1,inboxMessages.countByThreadIdAndSenderRole(id,"admin"));
  JsonNode delivered=getJson("/api/inbox/"+id,owner.session);assertEquals(draft,delivered.path("messages").get(1).path("body").asText());assertTrue(delivered.path("thread").path("unread").asBoolean());assertEquals(10,donations.count());
 }
 @Test void memberReplyCreatesANewDraftAndStaleDraftCannotSend() throws Exception {
  Entry owner=enter("conversation@example.org"),stranger=enter("outsider-conversation@example.org");MockHttpSession admin=admin();String key=UUID.randomUUID().toString();String create=json.writeValueAsString(Map.of("subject","Family support question","body","Please help with the request process.","clientId",key));
  String id=json.readTree(mvc.perform(post("/api/inbox").session(owner.session).with(csrf()).contentType("application/json").content(create)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).path("id").asText();
  mvc.perform(post("/api/inbox").session(owner.session).with(csrf()).contentType("application/json").content(create)).andExpect(jsonPath("$.id").value(id));assertEquals(1,inboxThreads.count());
  JsonNode first=getJson("/api/admin/inbox/"+id,admin);int oldRevision=first.path("thread").path("draftRevision").asInt();long oldSeen=first.path("thread").path("updatedAt").asLong();String reply=json.writeValueAsString(Map.of("body","The hospital has changed to another location.","clientId",UUID.randomUUID().toString()));
  mvc.perform(post("/api/inbox/"+id+"/messages").session(stranger.session).with(csrf()).contentType("application/json").content(reply)).andExpect(status().isNotFound());
  mvc.perform(post("/api/inbox/"+id+"/messages").session(owner.session).with(csrf()).contentType("application/json").content(reply)).andExpect(status().isOk());mvc.perform(post("/api/inbox/"+id+"/messages").session(owner.session).with(csrf()).contentType("application/json").content(reply)).andExpect(jsonPath("$.alreadySent").value(true));assertEquals(2,inboxMessages.countByThreadIdAndSenderRole(id,"member"));
  mvc.perform(post("/api/admin/inbox/"+id+"/read").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("seenAt",oldSeen)))).andExpect(status().isOk());assertTrue(inboxThreads.findById(id).orElseThrow().adminUnread);
  mvc.perform(post("/api/admin/inbox/"+id+"/send").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("body","This is the older reply draft.","revision",oldRevision)))).andExpect(status().isConflict());assertEquals(0,inboxMessages.countByThreadIdAndSenderRole(id,"admin"));
  JsonNode current=getJson("/api/admin/inbox/"+id,admin);assertEquals(oldRevision+1,current.path("thread").path("draftRevision").asInt());mvc.perform(post("/api/admin/inbox/"+id+"/read").session(admin).with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("seenAt",current.path("thread").path("updatedAt").asLong())))).andExpect(status().isOk());assertFalse(inboxThreads.findById(id).orElseThrow().adminUnread);
 }
 @Test void notificationAutomationTracksBothApprovalsAndOnlyOneReceipt() throws Exception {
  Entry donor=enter("notice-donor@example.org"),receiver=enter("notice-receiver@example.org");String d=profile(donor.session,"Notice Donor","Maharashtra","Thane","9000000010");profile(receiver.session,"Notice Receiver","Maharashtra","Thane","9000000011");String match=request(receiver.session,d);
  JsonNode donorThread=getJson("/api/inbox?kind=donation",donor.session).path("threads").get(0);String donorInbox=donorThread.path("id").asText();String receiverInbox=getJson("/api/inbox?kind=donation",receiver.session).path("threads").get(0).path("id").asText();assertNotEquals(donorInbox,receiverInbox);
  approve(donor.session,match,true).andExpect(status().isOk());approve(donor.session,match,true).andExpect(status().isOk());approve(receiver.session,match,true).andExpect(status().isOk());received(receiver.session,match).andExpect(status().isOk());received(receiver.session,match).andExpect(status().isOk());
  assertEquals(4,inboxMessages.countByThreadIdAndSenderRole(donorInbox,"system"));assertEquals(4,inboxMessages.countByThreadIdAndSenderRole(receiverInbox,"system"));assertTrue(getJson("/api/inbox/"+donorInbox,donor.session).path("messages").get(3).path("body").asText().contains("Blood received confirmed"));mvc.perform(get("/api/inbox/"+donorInbox).session(receiver.session)).andExpect(status().isNotFound());
  JsonNode admin=getJson("/api/admin/inbox/"+donorInbox,admin());assertTrue(admin.path("thread").path("draftText").asText().contains("500 recognition points"));assertEquals(1,donations.count());
 }
}
