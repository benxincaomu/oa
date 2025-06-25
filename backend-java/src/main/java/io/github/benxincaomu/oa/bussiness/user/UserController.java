package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.consts.Const;
import io.github.benxincaomu.oa.base.security.TokenValue;
import io.github.benxincaomu.oa.base.security.TokenValueRepository;
import io.github.benxincaomu.oa.base.utils.StringGenerator;
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

    // @Resource
    // private RedisTemplate<String, TokenValue> tokenRedisTemplate;

    @Resource
    private TokenValueRepository tokenValueRepository;

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
    public PagedModel<User> users(Integer currPage, Integer pageSize, String userName, String name) {
        PagedModel<User> page = new PagedModel<>(userService.users(currPage, pageSize, userName, name));
        return page;
    }

    @PostMapping()
    public void saveUser(@RequestBody User user) {
        userService.saveUser(user);
    }

    @GetMapping("{id}")
    public User getUser(@PathVariable Long id) {
        if (id == null || id < 1) {
            throw new RuntimeException("user id can't be null.");
        }

        return userService.getUserById(id);
    }

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

        tokenValue.setUserName(user.getUserName());
        // 获取角色权限
        Role role = roleRepository.findByUserId(user.getId());
        tokenValue.setRole(role);
        if (role != null) {

            List<Permission> permissions = permissionRepository.findByRoleId(role.getId());
            tokenValue.setPermissions(permissions);
        }

        String token = StringGenerator.generate(28) + System.currentTimeMillis();

        // tokenRedisTemplate.opsForValue().set(Const.TOKEN_KEY_PREFIX+token, tokenValue);

        tokenValue.setToken(token);
        tokenValueRepository.save(tokenValue);
        redisTemplate.opsForValue().set(Const.UID_TOKEN_PREFIX+user.getId(), token);

        return token;
        

    }
}
