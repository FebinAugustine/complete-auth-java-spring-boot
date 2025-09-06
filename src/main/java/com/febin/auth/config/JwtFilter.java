package com.febin.auth.config;

import com.febin.auth.util.CookieUtil;
import com.febin.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtFilter extends HttpFilter {

    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public JwtFilter(CookieUtil cookieUtil, JwtUtil jwtUtil, UserDetailsService userDetailsService) {
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        cookieUtil.getCookie(req, CookieUtil.ACCESS_TOKEN_COOKIE).ifPresent(cookie -> {
            String token = cookie.getValue();
            try {
                Claims claims = jwtUtil.parseClaims(token).getBody();
                String username = claims.getSubject();
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception e) {
                // Invalid/expired token -> ignore, request will be unauthorized
            }
        });
        chain.doFilter(req, res);
    }
}
