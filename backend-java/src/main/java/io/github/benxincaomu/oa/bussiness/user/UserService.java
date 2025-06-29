package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.response.OaResponseCode;
import io.github.benxincaomu.oa.base.utils.StringGenerator;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private UserRepository userRepository;

    @Resource
    private TenantRepository tenantRepository;

    @Resource
    private UserRoleRepository userRoleRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private PermissionRepository permissionRepository;

    public int updateUser(User user) {
        user.setPassword(null);
        return userRepository.save(user).getId() == null ? 0 : 1;
    }

    public String insert(User user) {
        Asserts.isFalse(userRepository.existsByUserName(user.getUserName()), OaResponseCode.USER_NAME_EXITS);
        user.setEnable(true);
        String salt = StringGenerator.generate(8);
        user.setSalt(salt);    
        String password = null; 
        if(user.getPassword() == null){
            password = StringGenerator.generate(8);
            user.setPassword(password);
        }else{
            password = user.getPassword();
        }
        user.setPassword(DigestUtils.md5Hex(user.getPassword() + salt));
        userRepository.save(user);
        logger.info("inserted user \"{}\",id={}",user.getUserName(),user.getId());
        return password;
    }

    public Page<User> users(Integer currPage, Integer pageSize, String userName, String name) {
        currPage = currPage == null || currPage < 0 ? 0 : currPage;
        pageSize = pageSize == null || pageSize < 1 ? 20 : currPage;

        PageRequest page = PageRequest.of(currPage, pageSize, Sort.by("createAt"));

        User user = new User();
        ExampleMatcher matcher = ExampleMatcher.matching();
        if (userName != null && !userName.isEmpty()) {
            matcher.withMatcher("userName", m -> m.exact());
            user.setUserName(userName);
        }
        if (name != null && !name.isEmpty()) {
            matcher.withMatcher("name", m -> m.exact());
            user.setName(name);
        }
        matcher.withMatcher("enable", m-> m.exact());
        user.setEnable(true);
        Example<User> example = Example.of(user, matcher);
        Page<User> users = userRepository.findAll(example, page);
        return users;
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id).get();
        return user;
    }


    public User findByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }


    /**
     * 更改用户状态
     * @param id 用户id
     * @param enable 用户状态
     * @return
     */
    @Transactional
    public int changeUserEnable(Long id, boolean enable) {
        userRepository.updateEnable(id, enable);

        return 0;
    }

    /**
     * 分配用户角色
     * @param userId 用户id
     * @param roleId 角色id
     */
    public void dispatcherRole(Long userId, Long roleId) {
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);

        userRoleRepository.save(userRole);

    }

    /**
     * 获取当前用户信息参数中的userId和userName只会使用一个，优先使用userId
     * @param userId 用户id
     * @param userName 用户名
     * @return
     */
    public User getFullInfoUser(Long userId,String userName){
        User user ;
        if(userId != null){ 
            user = userRepository.findById(userId).orElse(null);
        }else if(userName != null){
            user = userRepository.findByUserName(userName);
        }else{
            return null;
        }
        if(user != null){ 
            user.setRole(roleRepository.findByUserId(user.getId()));
            if(user.getRole() != null){
                user.getRole().setPermissions(permissionRepository.findByRoleId(user.getRole().getId()));
            }
            
        }
        return user;
    }
}
