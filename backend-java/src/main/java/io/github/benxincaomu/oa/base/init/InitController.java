package io.github.benxincaomu.oa.base.init;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.init.vo.InitVo;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.User;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * 初始化相关
 */
@RestController
@RequestMapping("initProject")
public class InitController {

    @Resource
    private InitService initService;

    @PostMapping()
    public void init(@RequestBody @Valid InitVo initVo) {
        Department department = new Department();
        department.setName(initVo.getDepartment().getName());
        department.setCode(initVo.getDepartment().getCode());
        User user = new User();
        user.setUserName(initVo.getUser().getUserName());
        user.setName(initVo.getUser().getName());
        user.setEmail(initVo.getUser().getEmail());
        user.setMobile(initVo.getUser().getMobile());
        user.setPassword(initVo.getUser().getPassword());
        Role role = new Role();
        role.setName(initVo.getRole().getName());
        initService.init(department, user, role);
    }

    
    @GetMapping("isInitialized")
    public boolean isInitialized() {
        return initService.isInitialized();
    }

}
