package io.github.benxincaomu.oa.base.security;

public final class SecurityConsts {
    private SecurityConsts(){}

    public static final String[] PUBLIC_URLS = {
            "/error",
            "/actuator/beans",
            "/actuator/health",
            "/initProject/**"
    };
    
}
