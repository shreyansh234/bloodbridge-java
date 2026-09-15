package com.bloodbridge;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
public class AbsoluteSessionExpiryFilter extends OncePerRequestFilter {
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {HttpSession s=req.getSession(false);if(s!=null){Object expiry=s.getAttribute("bb_session_expiry");if(expiry instanceof Long e&&e<=System.currentTimeMillis()){s.invalidate();SecurityContextHolder.clearContext();}}chain.doFilter(req,res);}
}
