package com.bloodbridge;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
@Component
public class RateLimiter {
 private record Window(long start,int count) {}
 private final ConcurrentHashMap<String,Window> counts=new ConcurrentHashMap<>();
 public void check(String key,int max,long duration) {
  long now=System.currentTimeMillis();
  if(counts.size()>10000)counts.entrySet().removeIf(e->now-e.getValue().start()>3600000);
  if(counts.size()>20000&&!counts.containsKey(key))throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Please try again later.");
  Window result=counts.compute(key,(k,w)->w==null||now-w.start()>=duration?new Window(now,1):new Window(w.start(),w.count()+1));
  if(result.count()>max)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Too many attempts. Please try again later.");
 }
}
