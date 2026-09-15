package com.bloodbridge;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
public class HealthController {
 private final AccountRepository accounts;
 public HealthController(AccountRepository accounts){this.accounts=accounts;}
 @GetMapping("/api/health") public Map<String,String> health(){accounts.count();return Map.of("status","ok","backend","java");}
}
