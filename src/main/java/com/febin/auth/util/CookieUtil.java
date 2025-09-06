package com.febin.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieUtil {

    public static final String ACCESS_TOKEN_COOKIE = "ATK";
    public static final String REFRESH_TOKEN_COOKIE = "RTK";

    public Cookie createCookie(String name, String value, int maxAgeSec, boolean httpOnly, boolean secure, String sameSite) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAgeSec);
        // SameSite can't be set via standard API prior to Servlet 4.0; append via header when adding
        return cookie;
    }

    public void addCookie(HttpServletResponse res, Cookie cookie, String sameSite) {
        // Add cookie normally
        res.addCookie(cookie);
        // Also add Set-Cookie header to ensure SameSite attribute is present (safe approach)
        StringBuilder sb = new StringBuilder();
        sb.append(cookie.getName()).append("=").append(cookie.getValue()).append("; Path=").append(cookie.getPath());
        if (cookie.getMaxAge() > 0) sb.append("; Max-Age=").append(cookie.getMaxAge());
        if (cookie.getSecure()) sb.append("; Secure");
        if (cookie.isHttpOnly()) sb.append("; HttpOnly");
        if (sameSite != null) sb.append("; SameSite=").append(sameSite);
        res.addHeader("Set-Cookie", sb.toString());
    }

    public Optional<Cookie> getCookie(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return Optional.empty();
        return Arrays.stream(req.getCookies()).filter(c -> c.getName().equals(name)).findFirst();
    }

    public void deleteCookie(HttpServletResponse res, String name, String sameSite) {
        Cookie cookie = new Cookie(name, null);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        res.addCookie(cookie);
        // also set header for SameSite if needed
        String header = name + "=; Path=/; Max-Age=0; HttpOnly; SameSite=" + sameSite;
        res.addHeader("Set-Cookie", header);
    }
}
