package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PagedModel;
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
import io.github.benxincaomu.oa.base.security.SaltedUser;
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
    public User insert(@RequestBody User user) {
        return userService.insert(user);
    }

    @DeleteMapping("{id}")
    public void disableUser(@PathVariable Long id) {
        userService.changeUserEnable(id, false);
    }

    @PutMapping
    public void updateUser(@RequestBody User user) {
        userService.updateUser(user);
    }

    @GetMapping("{id}")
    public User getUser(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new RuntimeException("user id can't be null.");
        }

        return userService.getUserById(id);
    }

    



    @GetMapping("currUser")
    public User getCurrUser(){
        Object details = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(details instanceof SaltedUser){ 
            SaltedUser user = (SaltedUser) details;
            return userService.getFullInfoUser(user.getUserId(), null);
        }
        return null;
    }

    

   


    @GetMapping("/getRoleIdByUserId/{userId}")
    public Long getRoleIdByUserId(@PathVariable("userId") Long userId){
        return userService.getRoleIdByUserId(userId);

    }

    /**
     * 分配角色
     * @param userRole 用户角色
     */
    @PostMapping("/assignRole")
    public void assignRole(@RequestBody UserRole userRole){
        userService.assignRole(userRole);
    }

  

    @GetMapping("/getDeptIdByUserId/{userId}")
    public Long getDeptIdByUserId(@PathVariable("userId") Long userId){
        return userService.getDeptIdByUserId(userId);
    }

    @PostMapping("/assignDept")
    public void assignDept(@RequestBody DepartmentUser departmentUser){
        userService.assignDept(departmentUser);
    }

    @PostMapping("/setPassword")
    public void setPassword(@Validated @RequestBody SetPasswordVo setPasswordVo){
        String pwdId = redisTemplate.opsForValue().get(setPasswordVo.getId());
        Asserts.isFalse(pwdId == null,OaResponseCode.UPDATE_PASSWORD_EXPIRED);
        Long id = Long.valueOf(pwdId);
        User user = new User();
        user.setId(id);
        user.setPassword(setPasswordVo.getPassword());
        userService.updatePassword(user);
        redisTemplate.delete(setPasswordVo.getId());
        ;
    }
    @GetMapping("/findUsersByNameLike/{name}")
    public List<UserIdNameVo> findUsersByNameLike(@PathVariable("name")String name){

        return userService.findUsersByNameLike(name);
    }

    @GetMapping("/getAllUsers")
    public List<UserIdNameVo> getAllUsers(){

        return userService.getAllUsers();
    }

}
