package io.github.benxincaomu.oa.bussiness.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PagedModel;
import org.springframework.lang.NonNull;
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

import io.github.benxincaomu.oa.base.consts.Const;
import io.github.benxincaomu.oa.base.security.SaltedUser;
import io.github.benxincaomu.oa.base.security.TokenValue;
import io.github.benxincaomu.oa.base.security.TokenValueRepository;
import io.github.benxincaomu.oa.base.utils.StringGenerator;
import io.github.benxincaomu.oa.bussiness.user.vo.SimpleUserInfo;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("user")
public class UserController {

    private final UserRepository userRepository;

    @Resource
    private UserService userService;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private PermissionRepository permissionRepository;

    @Resource
    private TokenValueRepository tokenValueRepository;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * 
     * @param currPage 当前页
     * @param pageSize 每页数量
     * @param userName 用户名
     * @param name     用户姓名
     * @return
     */
    @GetMapping("list")
    public PagedModel<User> users(Integer currPage, Integer pageSize, String userName, String name) {
        PagedModel<User> page = new PagedModel<>(userService.users(currPage, pageSize, userName, name));
        return page;
    }

    /**
     * 新增用户
     * 
     * @param user 用户信息
     */
    @PostMapping()
    public String insert(@RequestBody User user) {
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

    /**
     * 登录
     * 
     * @param login 登录信息
     * @return token
     */
    @PostMapping("login")
    public String login(@RequestBody User login) {
        logger.info("loggin");
        User user = userService.findByUserName(login.getUserName());
        if (user == null) {
            throw new RuntimeException("user not found.");
        }
        String password = DigestUtils.md5Hex(login.getPassword() + user.getSalt());
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("password error.");
        }
        TokenValue tokenValue = new TokenValue();
        tokenValue.setUserId(user.getId());

        tokenValue.setUserName(user.getUserName());
        // 获取角色权限
        Role role = roleRepository.findByUserId(user.getId());
        tokenValue.setRole(role);
        if (role != null) {

            List<Permission> permissions = permissionRepository.findByRoleId(role.getId());
            tokenValue.setPermissions(permissions);
        }else{
            tokenValue.setPermissions(new ArrayList<Permission>());
        }

        String token = StringGenerator.generate(28) + System.currentTimeMillis();


        tokenValue.setToken(token);
        tokenValue.setSalt(user.getSalt());
        tokenValueRepository.save(tokenValue);
        redisTemplate.opsForValue().set(Const.UID_TOKEN_PREFIX + user.getId(), token);

        return token;

    }

    /**
     * 分配角色
     * 
     * @param userId 用户id
     * @param roleId 角色id
     */
    @PostMapping("dispatcherRole")
    public void dispatcherRole(@Validated @NonNull Long userId,  @Validated @NonNull Long roleId) {
        userService.dispatcherRole(userId, roleId);
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

    @GetMapping("/myInfo")
    public SimpleUserInfo myInfo(){
        Object details = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(details instanceof SaltedUser){ 
            SaltedUser user = (SaltedUser) details;
            User u = userService.getUserById(user.getUserId());
            SimpleUserInfo simpleUserInfo = new SimpleUserInfo();
            simpleUserInfo.setName(u.getName());
            simpleUserInfo.setToken(((TokenValue)SecurityContextHolder.getContext().getAuthentication().getCredentials()).getToken());
            simpleUserInfo.setUserName(u.getUserName());
            return simpleUserInfo;
        }
        return null;
    }

}
