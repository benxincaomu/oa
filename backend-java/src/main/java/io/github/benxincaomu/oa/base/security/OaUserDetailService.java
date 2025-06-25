package io.github.benxincaomu.oa.base.security;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserService;
import jakarta.annotation.Resource;

// @Service
public class OaUserDetailService implements UserDetailsService {

    @Resource
    private UserService userService;
    private final Logger logger = LoggerFactory.getLogger(OaUserDetailService.class);
    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        logger.info("loadUserByUsername:"+userName);
        User user = userService.findByUserName(userName);
        if(user == null){
            throw new UsernameNotFoundException("User Not Found:"+userName);
        }
        // SecurityContextHolder.getContext().getAuthentication().getName();
        // throw new UnsupportedOperationException("Unimplemented method 'loadUserByUsername'");

        return new SaltedUser(user.getUserName(), user.getPassword(), true, true, true, true, null,user);
    }

}
