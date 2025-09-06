package com.febin.auth.security;

import com.febin.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple filter that extracts JWT access token from cookie named "ATK",
 * validates it with JwtUtil, and sets Authentication in SecurityContext.
 *
 * - If token missing/invalid -> leaves context unauthenticated.
 * - Roles are expected to be in claim "roles" as array/collection of strings.
 *
 * Ensure this filter runs BEFORE UsernamePasswordAuthenticationFilter.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final String accessCookieName;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, String accessCookieName) {
        this.jwtUtil = jwtUtil;
        this.accessCookieName = accessCookieName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            Optional<Cookie> atkCookie = getCookie(request, accessCookieName);
            if (atkCookie.isPresent()) {
                String token = atkCookie.get().getValue();
                if (token != null && !token.isBlank() && !jwtUtil.isTokenExpired(token)) {
                    Jws<io.jsonwebtoken.Claims> parsed = jwtUtil.parseClaims(token);
                    Claims claims = parsed.getBody();
                    String subject = claims.getSubject();
                    Object rolesObj = claims.get("roles");

                    List<SimpleGrantedAuthority> authorities = parseRoles(rolesObj);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(subject, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        } catch (Exception ex) {
            // If anything goes wrong while parsing token, do not set authentication.
            // Optionally log this in real app.
        }

        filterChain.doFilter(request, response);
    }

    private Optional<Cookie> getCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        return Arrays.stream(req.getCookies()).filter(c -> c.getName().equals(name)).findFirst();
    }

    @SuppressWarnings("unchecked")
    private List<SimpleGrantedAuthority> parseRoles(Object rolesObj) {
        if (rolesObj == null) return Collections.emptyList();
        Collection<?> collection;
        if (rolesObj instanceof Collection) {
            collection = (Collection<?>) rolesObj;
        } else if (rolesObj.getClass().isArray()) {
            collection = Arrays.asList((Object[]) rolesObj);
        } else {
            // single role string
            return List.of(new SimpleGrantedAuthority(String.valueOf(rolesObj)));
        }
        return collection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}
