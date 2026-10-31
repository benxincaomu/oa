package io.github.benxincaomu.oa.base.security;

import java.io.IOException;

import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benxincaomu.notry.exception.handler.ResponseMessage;
import io.github.benxincaomu.oa.base.init.InitService;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Rejects business requests until startup provisioning of the Keycloak
 * role/resource/permission model has succeeded. There is no initialization page
 * any more: the application provisions itself at startup and only health checks
 * remain reachable while it is not ready.
 */
public class ProjectInitializationFilter extends OncePerRequestFilter {
    private static final String[] PUBLIC_PATTERNS = { "/actuator/health" };

    private final InitService initService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ProjectInitializationFilter(InitService initService, ObjectMapper objectMapper) {
        this.initService = initService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (initService.isReady() || isPublicUrl(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ResponseMessage<>(OaResponseCode.PROJECT_NOT_INITIALIZED, null)));
    }

    private boolean isPublicUrl(String servletPath) {
        for (String pattern : PUBLIC_PATTERNS) {
            if (pathMatcher.match(pattern, servletPath)) {
                return true;
            }
        }
        return false;
    }
}
