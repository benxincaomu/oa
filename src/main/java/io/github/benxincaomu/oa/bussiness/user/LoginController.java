package io.github.benxincaomu.oa.bussiness.user;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.consts.Const;
import io.github.benxincaomu.oa.base.security.SaltedUser;
import io.github.benxincaomu.oa.base.security.TokenValue;
import io.github.benxincaomu.oa.base.security.TokenValueRepository;
import io.github.benxincaomu.oa.base.utils.StringGenerator;
import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import io.github.benxincaomu.oa.bussiness.user.vo.SimpleUserInfo;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("login")
public class LoginController {
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
    private TokenValueRepository tokenValueRepository;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 登录
     * 
     * @param login 登录信息
     * @return token
     */
    @PostMapping("login")
    public String login(@RequestBody User login,HttpServletResponse response) {
        logger.info("loggin");
        User user = userService.findByUserName(login.getUserName());
        if (user == null) {
            throw new RuntimeException("username or password error.");
        }
        String password = DigestUtils.md5Hex(login.getPassword() + user.getSalt());
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("username or password error.");
        }
        TokenValue tokenValue = new TokenValue();
        tokenValue.setUserId(user.getId());

        tokenValue.setUserName(user.getUserName());
        // 获取角色权限
        Role role = roleRepository.findByUserId(user.getId());
        tokenValue.setRole(role);
        if (role != null) {

            List<PermissionIdName> permissions = permissionService.getMenuTreeByRoleId(role.getId());
            Set<String> urls = new HashSet<>();
            Stack<PermissionIdName> stack = new Stack<>();
            Iterator<PermissionIdName> iterator2 = permissions.iterator();
            while (iterator2.hasNext()) { 
                PermissionIdName permission = iterator2.next(); 
                if (permission.getType() == Permission.TYPE_3) { 
                    urls.add(permission.getValue());
                    iterator2.remove();
                } else { 
                    stack.push(permission);
                }
            }
            
            while (!stack.isEmpty()) { 
                PermissionIdName permission = stack.pop();
                
                List<PermissionIdName> children = permission.getChildren();
                if(children!=null){

                    Iterator<PermissionIdName> iterator = children.iterator();
                    while (iterator.hasNext()) {
                        PermissionIdName next = iterator.next();
                        if(next.getType() == Permission.TYPE_3){
                            urls.add(next.getValue());
                            iterator.remove();
                        }else if(next.getType() == Permission.TYPE_1 || next.getType() == Permission.TYPE_2){
                            stack.push(next);
                        }
                    }
                }
            }
            tokenValue.setUrls(urls);
            
            tokenValue.setMenus(permissions);
        }else{
            tokenValue.setMenus(new ArrayList<PermissionIdName>());
        }

        String token = StringGenerator.generate(28) + System.currentTimeMillis();


        tokenValue.setToken(token);
        tokenValue.setSalt(user.getSalt());
        tokenValue.setTenantId(user.getTenantId());
        tokenValueRepository.save(tokenValue);
        redisTemplate.opsForValue().set(Const.UID_TOKEN_PREFIX + user.getId(), token);
        // Base64.getEncoder().encodeToString(user.getName().getBytes());
        Cookie nameCookie = new Cookie("name", Base64.getEncoder().encodeToString(user.getName().getBytes(StandardCharsets.UTF_8)));
        nameCookie.setMaxAge(60 * 60 * 24 * 30);
        nameCookie.setPath("/");
        response.addCookie(nameCookie);
        Cookie cookie = new Cookie("token", token);
        cookie.setMaxAge(60 * 60 * 24 * 30);
        cookie.setPath("/");
        response.addCookie(cookie);
        return token;

    }

    @GetMapping("/myInfo")
    public SimpleUserInfo myInfo() {
        Object details = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (details instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) details;
            User u = userService.getUserById(user.getUserId());
            SimpleUserInfo simpleUserInfo = new SimpleUserInfo();
            simpleUserInfo.setName(u.getName());
            simpleUserInfo.setToken(
                    ((TokenValue) SecurityContextHolder.getContext().getAuthentication().getCredentials()).getToken());
            simpleUserInfo.setUserName(u.getUserName());
            return simpleUserInfo;
        }
        return null;
    }

    @GetMapping("/getMyMenus")
    public List<PermissionIdName> getMyMenus() {

        return ((TokenValue) SecurityContextHolder.getContext().getAuthentication().getCredentials()).getMenus();
    }


      @PostMapping("/logout")
    public void logout(@CookieValue(name = "token",required = false) String token){
        if(token != null){
            tokenValueRepository.findById(token).ifPresent(tokenVlue ->{
                redisTemplate.delete(Const.UID_TOKEN_PREFIX + tokenVlue.getUserId());
                logger.info("logout userId:{}",tokenVlue.getUserId());
            });
            tokenValueRepository.deleteById(token);
        }

    }
}
