package io.github.benxincaomu.oa.base.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benxincaomu.notry.exception.handler.ResponseMessage;
import io.github.benxincaomu.oa.base.init.InitService;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.user.Role;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Resource
    private TokenValueRepository tokenValueRepository;

    private Set<String> excludeUrls = new HashSet<>();

    @Resource
    private InitService initService;

    @PostConstruct
    public void init() {
        for (int i = 0; i < SecurityConsts.PUBLIC_URLS.length; i++) {
            excludeUrls.add(SecurityConsts.PUBLIC_URLS[i]);
        }
    }

    @SuppressWarnings("null")
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 判断项目是否已经初始话，未初始化要跳转到初始化页面
        String servletPath = request.getServletPath();
        if (!initService.isInitialized() && !(servletPath.equals("/initProject")&& request.getMethod().equals("POST"))) {

            String referer = request.getHeader("Referer");
            // String userAgent = request.getHeader("User-Agent");
            // String origin = request.getHeader("Origin");
            if (referer != null && referer.endsWith("/init")) {
                return;
            }

            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.PROJECT_NOT_INITIALIZED,
                    null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();

            return;
        }
        // String servletPath = request.getServletPath();
        AntPathMatcher matcher = new AntPathMatcher();
        for (String url : SecurityConsts.PUBLIC_URLS) {
            if (matcher.match(url, servletPath)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        // String token = request.getHeader("token");
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    token = cookie.getValue();
                    break;
                }
            }
        }
        if (token == null) {
            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.TOKEN_NOT_EXIST, null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();
            return;
        }
        Optional<TokenValue> tokenValue = tokenValueRepository.findById(token);
        if (!tokenValue.isPresent()) {
            // throw new BadCredentialsException("token不存在");
            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.TOKEN_NOT_EXIST, null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();
            return;
        }
        TokenValue tv = tokenValue.get();

        Set<String> urls = tv.getUrls();
        if (urls == null || !urls.stream().anyMatch(url -> matcher.match(url, servletPath))) {
            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.NO_PERMISSION, null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();
            return;
        }

        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        Role role = tv.getRole();
        SaltedUser user = new SaltedUser(tv.getUserName(), tv.getSalt(), true, true, true, true, authorities,
                tv.getUserId(), tv.getSalt(), tv.getTenantId(), role.getId());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, tv, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

}
