package io.github.benxincaomu.oa.base.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benxincaomu.notry.exception.handler.ResponseMessage;
import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import io.micrometer.core.ipc.http.HttpSender.Response;
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
        if(token == null){
            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.TOKEN_NOT_EXIST,null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();
            return;
        }
        Optional<TokenValue> tokenValue = tokenValueRepository.findById(token);
        if (!tokenValue.isPresent()) {
            // throw new BadCredentialsException("token不存在");
            ResponseMessage<String> responseMessage = new ResponseMessage<>(OaResponseCode.TOKEN_NOT_EXIST,null);
            response.setHeader("Content-Type", "application/json;charset=UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            response.getWriter().write(objectMapper.writeValueAsString(responseMessage));
            response.getWriter().flush();
            return;
        } 
        // Asserts.isTrue(tokenValue.isPresent(), OaResponseCode.TOKEN_NOT_EXIST);
        TokenValue tv = tokenValue.get();
        // Role role = tokenValue.get().getRole();
        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        /* List<PermissionIdName> permissions = tv.getPermissions();
        if (permissions != null && permissions.size() > 0) {
            permissions.forEach(permission -> {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(permission.getName());
                authorities.add(authority);
            });
        } */
        SaltedUser user = new SaltedUser(tv.getUserName(), tv.getSalt(), true, true, true, true, authorities, tv.getUserId(),tv.getSalt(),tv.getTenantId());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, tv, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

}
