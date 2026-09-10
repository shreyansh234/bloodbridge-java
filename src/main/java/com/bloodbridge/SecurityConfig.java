package com.bloodbridge;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.authentication.session.*;
import java.util.List;
import jakarta.servlet.DispatcherType;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder() {return new BCryptPasswordEncoder(12);}
 @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
  return email->accounts.findByEmail(email).map(a->User.withUsername(a.email).password(a.passwordHash).roles("USER").build()).orElseThrow(()->new UsernameNotFoundException("Invalid email or password."));
 }
 @Bean AuthenticationManager authenticationManager(UserDetailsService users,PasswordEncoder encoder) {
  DaoAuthenticationProvider provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);return new ProviderManager(provider);
 }
 @Bean SecurityContextRepository contextRepository() {return new HttpSessionSecurityContextRepository();}
 @Bean HttpSessionCsrfTokenRepository csrfRepository() {return new HttpSessionCsrfTokenRepository();}
 @Bean SessionAuthenticationStrategy sessionStrategy(HttpSessionCsrfTokenRepository csrf) {return new CompositeSessionAuthenticationStrategy(List.of(new ChangeSessionIdAuthenticationStrategy(),new CsrfAuthenticationStrategy(csrf)));}
 @Bean SecurityFilterChain securityFilterChain(HttpSecurity http,SecurityContextRepository contexts,HttpSessionCsrfTokenRepository csrf) throws Exception {
  http.authorizeHttpRequests(auth->auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll().requestMatchers("/","/index.html","/assets/**","/favicon.svg","/api/session","/api/donors","/api/auth/login","/api/auth/register").permitAll().requestMatchers("/api/**").authenticated().anyRequest().denyAll())
   .securityContext(context->context.securityContextRepository(contexts).requireExplicitSave(true))
   .csrf(config->config.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
   .requestCache(cache->cache.disable()).formLogin(form->form.disable()).httpBasic(basic->basic.disable())
   .logout(logout->logout.disable())
   .exceptionHandling(ex->ex.authenticationEntryPoint((req,res,err)->{res.setStatus(401);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Sign in to continue.\"}");}).accessDeniedHandler((req,res,err)->{res.setStatus(403);res.setContentType("application/json");res.getWriter().write("{\"error\":\"Your session has changed. Refresh the page and try again.\"}");}))
   .headers(headers->headers.contentSecurityPolicy(csp->csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; font-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
  return http.build();
 }
}
