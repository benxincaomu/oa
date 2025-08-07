package io.github.benxincaomu.oa.base.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class OaAuthManager implements AuthenticationManager {

    private final Logger logger = LoggerFactory.getLogger(OaAuthManager.class);
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        logger.info("authenticate:"+authentication.getName());
        
        return authentication;
    }

}
