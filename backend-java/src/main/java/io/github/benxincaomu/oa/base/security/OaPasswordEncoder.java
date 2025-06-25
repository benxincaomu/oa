package io.github.benxincaomu.oa.base.security;

import java.security.Security;
import java.util.Objects;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserService;
import jakarta.annotation.Resource;

// @Component
public class OaPasswordEncoder  implements PasswordEncoder{

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private UserService userService;

    private ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

    @Override
    public String encode(CharSequence rawPassword) {
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        logger.info("userName:{}",userName);
        User user = userService.findByUserName(userName);
        if(user == null){
            throw new RuntimeException("user not found:"+userName);
        }
        // userThreadLocal.set(user);
        return DigestUtils.md5Hex(rawPassword.toString()+user.getSalt());

        // throw new UnsupportedOperationException("Unimplemented method 'encode'");
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        // User user = userThreadLocal.get();
        // userThreadLocal.remove();
        return true;
        
    }

}
