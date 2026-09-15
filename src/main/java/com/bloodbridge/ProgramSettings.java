package com.bloodbridge;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
@Component
public class ProgramSettings {
 private final Set<String> admins;
 public final boolean assistanceEnabled;
 public ProgramSettings(@Value("${bloodbridge.admin-ids:}") String ids,@Value("${bloodbridge.family-assistance-enabled:false}") boolean enabled){admins=new HashSet<>();for(String id:ids.split(","))if(!id.isBlank())admins.add(id.trim());assistanceEnabled=enabled;}
 public boolean isAdmin(Account account){return "admin".equals(account.provider)||(account.externalId!=null&&admins.contains("supabase:"+account.externalId));}
}
