package io.github.benxincaomu.oa.bussiness.user;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;

import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.utils.StringGenerator;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.mail.MailService;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUser;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;
// import static java.lang.StringTemplate.STR;

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

    @Resource
    private DepartmentUserRepository departmentUserRepository;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private MailService mailService;

    @Value("${app.base-url}")
    private String projectBaseUrl;

    public int updateUser(User user) {
        user.setPassword(null);
        return userRepository.save(user).getId() == null ? 0 : 1;
    }

    public String insert(User user) {
        Asserts.isFalse(userRepository.existsByUserName(user.getUserName()), OaResponseCode.USER_NAME_EXITS);
        if(user.getEmail()!=null){
            Asserts.isFalse(userRepository.existsByEmail(user.getEmail()), OaResponseCode.EMAIL_EXITS);
        }
        user.setEnable(true);
        String salt = StringGenerator.generate(8);
        user.setSalt(salt);    
        if(user.getPassword() != null){
            user.setPassword(DigestUtils.md5Hex(user.getPassword() + salt));
        }
        userRepository.save(user);
        // 发送邮件
        String passwordSetId = StringGenerator.generate(20);
        if(user.getEmail()!=null){
            String content ="""
        <html>
        <body>
            <div>点击<a href="{0}/setPassword?id={1}">此处</a>为{2}修改密码</div>
        </body>
        </html>
        """;
            mailService.sendMail(user.getEmail(), "设置密码", MessageFormat.format(content, projectBaseUrl, passwordSetId, user.getName()));
        }

        redisTemplate.opsForValue().set(passwordSetId,user.getId()+"",Duration.ofHours(24));

        logger.info("inserted user \"{}\",id={}",user.getUserName(),user.getId());
        return null;
    }

    public Page<User> users(Integer currPage, Integer pageSize, String userName, String name) {
        currPage = currPage == null || currPage < 0 ? 0 : currPage - 1;
        pageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;

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
        matcher.withMatcher("enable", m -> m.exact());
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
     * 
     * @param id     用户id
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
     * 
     * @param userRole 用户角色
     */
    @Transactional
    public void assignRole(UserRole userRole) {
        userRoleRepository.deleteByUserId(userRole.getUserId());

        userRoleRepository.save(userRole);

    }

    /**
     * 获取当前用户信息参数中的userId和userName只会使用一个，优先使用userId
     * 
     * @param userId   用户id
     * @param userName 用户名
     * @return
     */
    public User getFullInfoUser(Long userId, String userName) {
        User user;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        } else if (userName != null) {
            user = userRepository.findByUserName(userName);
        } else {
            return null;
        }
        if (user != null) {
            user.setRole(roleRepository.findByUserId(user.getId()));
            if (user.getRole() != null) {
                user.getRole().setPermissions(permissionRepository.findByRoleId(user.getRole().getId()));
            }

        }
        return user;
    }

    public Long getRoleIdByUserId(Long userId) {
        return userRoleRepository.getRoleIdByUserId(userId);
    }

    public Long getDeptIdByUserId(Long userId) {
        return departmentUserRepository.findDeptIdByUserId(userId).orElse(0L);
    }

    @Transactional
    public void assignDept(DepartmentUser departmentUser) {
        departmentUserRepository.deleteByUserId(departmentUser.getUserId());
        departmentUserRepository.save(departmentUser);
    }

    
    @Transactional
    public void updatePassword(User user) {
        userRepository.findById(user.getId()).ifPresent(user1 -> { 
            user1.setPassword(DigestUtils.md5Hex(user.getPassword() + user1.getSalt()));
        });
        
    }

    public List<UserIdNameVo> findUsersByNameLike(String name) {
        return userRepository.findUsersByNameLike(name,JpaAuditorAware.getCurrentTenantId());
    }

    public Long findLeaderId(Long userId) {
        return userRepository.findLeaderId(userId);
    }
}
