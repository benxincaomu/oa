package io.github.benxincaomu.oa.base.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.Resource;

@Configuration
public class SecurityConfig {

    @Resource
    private SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .authorizeHttpRequests(ahr -> ahr.requestMatchers(HttpMethod.POST, SecurityConsts.PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated())
                .csrf(http -> http.disable())
                .formLogin(hs -> hs.disable())
                .httpBasic(hbc -> hbc.disable())
                .sessionManagement(ss -> ss.disable())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)

        ;
        return httpSecurity.build();
    }

}
