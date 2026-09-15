package com.bloodbridge;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class AccessService {
 private final AccountRepository accounts;private final ProgramSettings program;
 public AccessService(AccountRepository accounts,ProgramSettings program){this.accounts=accounts;this.program=program;}
 public Account account(Authentication auth){if(auth==null)throw fail(401,"Sign in to continue.");return accounts.findById(auth.getName()).orElseThrow(()->fail(401,"Sign in again."));}
 public Account admin(Authentication auth){Account a=account(auth);if(!program.isAdmin(a))throw fail(403,"Administrator access required.");return a;}
 public static ResponseStatusException fail(int code,String message){return new ResponseStatusException(HttpStatus.valueOf(code),message);}
}
