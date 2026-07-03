package com.campus.config;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SessionHelper {

    private static final String USER_ID_KEY = "currentUserId";
    private static final Long DEFAULT_USER_ID = 1L;

    public Long getCurrentUserId() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attr == null) return DEFAULT_USER_ID;
        HttpSession session = attr.getRequest().getSession(false);
        if (session == null || session.getAttribute(USER_ID_KEY) == null) return DEFAULT_USER_ID;
        return (Long) session.getAttribute(USER_ID_KEY);
    }

    public void setCurrentUserId(HttpSession session, Long userId) {
        session.setAttribute(USER_ID_KEY, userId);
    }
}
