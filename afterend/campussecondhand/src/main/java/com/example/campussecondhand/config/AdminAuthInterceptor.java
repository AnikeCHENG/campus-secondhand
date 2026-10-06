package com.example.campussecondhand.config;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端权限拦截器。
 *
 * <p>此前 {@code AdminController} 的 18 个端点各自手写
 * {@code if (!isAdmin(authHeader)) return 403}。这种方式有两个问题：
 * 新增端点时忘记写这一行就是越权漏洞；重复 18 处也难以保证判定逻辑始终一致。
 * 收敛到拦截器后，新端点自动受保护。</p>
 *
 * <p>角色只从 <b>JWT</b> 解析——JWT 由服务端用密钥签发，前端无法伪造。
 * 不信任任何请求参数或请求头中的角色声明。</p>
 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 只读请求之外的动词（CORS 预检）不拦截
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        if (!isAdmin(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(MAPPER.writeValueAsString(
                    ApiResponse.error(403, "无权限：需要管理员身份")));
            return false;
        }
        return true;
    }

    private boolean isAdmin(HttpServletRequest request) {
        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                return false;
            }
            Integer role = jwtUtil.getRoleFromToken(header.substring(7));
            return role != null && role == 1;
        } catch (Exception e) {
            // token 过期、被篡改等一律视为无权限，不向外抛异常细节
            return false;
        }
    }
}