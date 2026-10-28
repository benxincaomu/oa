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

public class ProjectInitializationFilter extends OncePerRequestFilter {
    private static final String[] INITIALIZATION_URLS = { "/initProject/**", "/actuator/health" };

    private final InitService initService;
    private final KeycloakConnectionService keycloakConnectionService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public ProjectInitializationFilter(InitService initService,
            KeycloakConnectionService keycloakConnectionService, ObjectMapper objectMapper) {
        this.initService = initService;
        this.keycloakConnectionService = keycloakConnectionService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 需要同时满足“已有租户数据”“已配置 Keycloak”“管理员/根部门/角色已建立 Keycloak 链接”才认为可以对外服务
        boolean ready = initService.isInitialized()
                && keycloakConnectionService.getConfiguration().isPresent()
                && initService.hasKeycloakLinks();
        if (ready || isInitializationUrl(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ResponseMessage<>(OaResponseCode.PROJECT_NOT_INITIALIZED, null)));
    }

    private boolean isInitializationUrl(String servletPath) {
        for (String pattern : INITIALIZATION_URLS) {
            if (pathMatcher.match(pattern, servletPath)) {
                return true;
            }
        }
        return false;
    }
}
