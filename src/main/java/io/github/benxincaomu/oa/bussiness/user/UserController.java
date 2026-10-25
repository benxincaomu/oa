package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.notry.utils.Asserts;
import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import io.github.benxincaomu.oa.base.security.SaltedUser;
import io.github.benxincaomu.oa.base.security.OaOidcUserPrincipal;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUser;
import io.github.benxincaomu.oa.bussiness.user.vo.SetPasswordVo;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private RoleRepository roleRepository;


    @Resource
    private PermissionRepository permissionRepository;

    @Resource
    private PermissionService permissionService;

    @Resource
    private RoleService roleService;

    @Resource
    private KeycloakDirectoryService directoryService;



    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     *
     * @param currPage 当前页
     * @param pageSize 每页数量
     * @param userName 用户名
     * @param name     用户姓名
     * @return
     */
    @GetMapping("list")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public PagedModel<User> list(Integer currPage, Integer pageSize, String userName, String name) {
        PagedModel<User> page = new PagedModel<>(userService.users(currPage, pageSize, userName, name));
        return page;
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     */
    @PostMapping()
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public User insert(@RequestBody User user) {
        throw new IllegalStateException("用户数据由 Keycloak 管理，请使用 /identity 接口");
    }

    @DeleteMapping("{id}")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public void disableUser(@PathVariable Long id) {
        throw new IllegalStateException("用户数据由 Keycloak 管理，请使用 /identity 接口");
    }

    @PutMapping
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public void updateUser(@RequestBody User user) {
        throw new IllegalStateException("用户数据由 Keycloak 管理，请使用 /identity 接口");
    }

    @GetMapping("{id}")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public User getUser(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new RuntimeException("user id can't be null.");
        }

        return userService.getUserById(id);
    }





    @GetMapping("currUser")
    public User getCurrUser(){
        Object details = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (details instanceof OaOidcUserPrincipal user) {
            return userService.getFullInfoUser(user.getOaUserId(), null);
        }
        if(details instanceof SaltedUser){
            SaltedUser user = (SaltedUser) details;
            return userService.getFullInfoUser(user.getUserId(), null);
        }
        return null;
    }






    @GetMapping("/getRoleIdByUserId/{userId}")
    public Long getRoleIdByUserId(@PathVariable("userId") Long userId){
        throw new IllegalStateException("角色关系由 Keycloak 管理，请使用 /identity 接口");

    }

    /**
     * 分配角色
     * @param userRole 用户角色
     */
    @PostMapping("/assignRole")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public void assignRole(@RequestBody UserRole userRole){
        throw new IllegalStateException("角色关系由 Keycloak 管理，请使用 /identity 接口");
    }



    @GetMapping("/getDeptIdByUserId/{userId}")
    public Long getDeptIdByUserId(@PathVariable("userId") Long userId){
        return directoryService.findDepartmentNumericIdsForUser(userId, currentTenantId()).stream()
                .findFirst()
                .orElse(0L);
    }

    @PostMapping("/assignDept")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public void assignDept(@RequestBody DepartmentUser departmentUser){
        throw new IllegalStateException("组织成员关系由 Keycloak 管理，请使用 /identity 接口");
    }

    @PostMapping("/setPassword")
    @PreAuthorize("hasAuthority('ROLE_oa-admin')")
    public void setPassword(@Validated @RequestBody SetPasswordVo setPasswordVo){
        throw new IllegalStateException("密码由 Keycloak 管理，请使用 /identity 接口");
    }
    @GetMapping("/findUsersByNameLike/{name}")
    public List<UserIdNameVo> findUsersByNameLike(@PathVariable("name")String name){

        return directoryService.listTenantUsersForSelection(currentTenantId(), name);
    }

    @GetMapping("/getAllUsers")
    public List<UserIdNameVo> getAllUsers(){

        return directoryService.listTenantUsersForSelection(currentTenantId(), null);
    }

    private Long currentTenantId() {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("An OA tenant context is required");
        }
        return tenantId;
    }

}
