package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Service
public class RoleService {
    @Resource
    private RoleRepository roleRepository;

    @PostConstruct
    public void initRole(){
        Role role = new Role();
        role.setName("管理员");
    }
}
