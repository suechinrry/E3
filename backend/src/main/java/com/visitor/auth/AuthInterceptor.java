package com.visitor.auth;

import com.visitor.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    // 各角色允许访问的路由（支持 * 单段通配和 ** 多段通配）
    private static final Map<String, List<String>> ROLE_ROUTES = Map.of(
        "admin", List.of(
            "/admin/**",
            "/appointment/host", "/appointment/host/**", "/appointment/helper",
            "/appointment/*/approve", "/appointment/*/qrcode",
            "/notification", "/ai/**",
            "/user/profile"
        ),
        "host", List.of(
            "/appointment/host", "/appointment/host/**",
            "/appointment/helper",
            "/appointment/*/approve",
            "/notification",
            "/ai/**",
            "/user/profile"
        ),
        "visitor", List.of(
            "/appointment",
            "/appointment/host/lookup",
            "/appointment/my",
            "/appointment/*/cancel",
            "/appointment/*/restore",
            "/appointment/*/rebook",
            "/appointment/*/qrcode",
            "/notification",
            "/ai/appointment/*/greeting",
            "/user/profile"
        ),
        "guard", List.of(
            "/guard/**",
            "/user/profile"
        )
    );

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或token缺失");
            return false;
        }

        String role;
        try {
            String token = authHeader.substring(7);
            Integer userId = jwtUtil.getUserId(token);
            role = jwtUtil.getRole(token);
            request.setAttribute("userId", userId);
            request.setAttribute("role", role);
        } catch (Exception e) {
            writeError(response, 401, "token无效或已过期");
            return false;
        }

        String uri = request.getRequestURI();
        List<String> allowed = ROLE_ROUTES.get(role);
        if (allowed == null) {
            writeError(response, 403, "未知角色，无权访问");
            return false;
        }

        for (String pattern : allowed) {
            if (matchRoute(pattern, uri)) {
                return true;
            }
        }

        log.warn("角色 {} 尝试越权访问: {}", role, uri);
        writeError(response, 403, "您没有权限访问该接口");
        return false;
    }

    /** 简单的路由匹配：* 匹配单段，** 匹配多段，其余精确匹配 */
    private boolean matchRoute(String pattern, String uri) {
        String regex = pattern
            .replace("**", "___DOUBLE_STAR___")
            .replace("*", "[^/]+")
            .replace("___DOUBLE_STAR___", ".*");
        return Pattern.matches(regex, uri);
    }

    private void writeError(HttpServletResponse response, int code, String msg) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build().writeValueAsString(Result.error(code, msg)));
    }
}
