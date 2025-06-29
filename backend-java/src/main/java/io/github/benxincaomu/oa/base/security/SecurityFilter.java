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
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.Role;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
        TokenValue tv = tokenValue.get();
        // Role role = tokenValue.get().getRole();
        List<Permission> permissions = tv.getPermissions();
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (permissions != null && permissions.size() > 0) {
            permissions.forEach(permission -> {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(permission.getValue());
                authorities.add(authority);
            });
        }
        SaltedUser user = new SaltedUser(tv.getUserName(), tv.getSalt(), true, true, true, true, authorities, tv.getUserId(),tv.getSalt());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, tv, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

}
