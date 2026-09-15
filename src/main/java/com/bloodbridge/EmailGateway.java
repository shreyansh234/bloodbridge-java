package com.bloodbridge;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
@Component
public class EmailGateway {
 private final ObjectMapper json;private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build();
 public EmailGateway(ObjectMapper json){this.json=json;}
 public String send(String key,String from,String to,String subject,String text,String idempotencyKey) throws Exception {
  String body=json.writeValueAsString(Map.of("from",from,"to",List.of(to),"subject",subject,"text",text));
  HttpResponse<String> response;
  try{response=client.send(HttpRequest.newBuilder(URI.create("https://api.resend.com/emails")).timeout(Duration.ofSeconds(12)).header("Authorization","Bearer "+key).header("Content-Type","application/json").header("Idempotency-Key",idempotencyKey).POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}catch(InterruptedException e){Thread.currentThread().interrupt();throw e;}
  if(response.statusCode()<200||response.statusCode()>=300)throw new IllegalStateException("Email provider did not accept the message.");return json.readTree(response.body()).path("id").asText("");
 }
}
