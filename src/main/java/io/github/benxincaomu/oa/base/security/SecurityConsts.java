package io.github.benxincaomu.oa.base.security;

public final class SecurityConsts {
    private SecurityConsts(){}

    public static final String[] PUBLIC_URLS = {
            "/user/login",
            "/user/logout",
            "/actuator/beans",
            "/user/setPassword",
            "/initProject/**"
            
    };
    
}
