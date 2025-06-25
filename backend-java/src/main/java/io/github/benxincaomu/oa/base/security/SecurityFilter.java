package io.github.benxincaomu.oa.base.security;

import java.io.IOException;
import java.security.Security;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import ch.qos.logback.core.subst.Token;
import io.github.benxincaomu.oa.base.consts.Const;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Resource
    private TokenValueRepository tokenValueRepository;

    private Set<String> excludeUrls = new HashSet<>();

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
        if (excludeUrls.contains(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("token");
        Optional<TokenValue> tokenValue = tokenValueRepository.findById(token);
        if (!tokenValue.isPresent()) {
            throw new RuntimeException("token不存在");
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                tokenValue.get().getUserName(), tokenValue.get(), null);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

}
