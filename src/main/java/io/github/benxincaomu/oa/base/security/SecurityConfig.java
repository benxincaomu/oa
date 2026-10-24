package io.github.benxincaomu.oa.base.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import io.github.benxincaomu.oa.base.init.InitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.SessionCookieConfig;


@Configuration
public class SecurityConfig {

    @Bean
    public ServletContextInitializer sessionCookieInitializer(
            @Value("${APP_SESSION_COOKIE_SECURE:true}") boolean secureCookie) {
        return servletContext -> {
            SessionCookieConfig cookie = servletContext.getSessionCookieConfig();
            cookie.setHttpOnly(true);
            cookie.setSecure(secureCookie);
            cookie.setPath("/");
            cookie.setAttribute("SameSite", "Lax");
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
            KeycloakClientRegistrationRepository clientRegistrationRepository,
            KeycloakOidcUserService oidcUserService,
            KeycloakAuthoritiesMapper authoritiesMapper,
            KeycloakLogoutSuccessHandler logoutSuccessHandler,
            ProjectInitializationFilter initializationFilter) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");

        httpSecurity
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SecurityConsts.PUBLIC_URLS).permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .oauth2Login(oauth2 -> oauth2
                        .clientRegistrationRepository(clientRegistrationRepository)
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(oidcUserService)
                                .userAuthoritiesMapper(authoritiesMapper))
                        .defaultSuccessUrl("/app", true))
                .logout(logout -> logout
                        .logoutUrl("/login/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .addFilterBefore(initializationFilter, LogoutFilter.class);

        return httpSecurity.build();
    }

    @Bean
    public ProjectInitializationFilter projectInitializationFilter(InitService initService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new ProjectInitializationFilter(initService, objectMapper);
    }

    private static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestAttributeHandler plain = new CsrfTokenRequestAttributeHandler();
        private final XorCsrfTokenRequestAttributeHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                Supplier<CsrfToken> deferredCsrfToken) {
            deferredCsrfToken.get();
            xor.handle(request, response, deferredCsrfToken);
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return StringUtils.hasText(headerValue)
                    ? plain.resolveCsrfTokenValue(request, csrfToken)
                    : xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
