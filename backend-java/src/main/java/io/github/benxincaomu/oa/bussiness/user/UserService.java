package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.utils.StringGenerator;
import io.github.benxincaomu.oa.bussiness.tenant.Tenant;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Service
public class UserService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Resource
    private UserRepository userRepository;

    @Resource
    private TenantRepository tenantRepository;


    public int saveUser(User user) {
        String salt = StringGenerator.generate(8);
        user.setSalt(salt);
        user.setPassword(DigestUtils.md5Hex(user.getPassword() + salt));
        return userRepository.save(user).getId() == null?0 :1;
    }

    public Page<User> users(Integer currPage, Integer pageSize, String userName, String name) {
        currPage = currPage == null || currPage < 0 ? 0 : currPage;
        pageSize = pageSize == null || pageSize < 1 ? 20 : currPage;

        PageRequest page = PageRequest.of(currPage, pageSize,Sort.by("createAt"));
        
        User user = new User();
        user.setUserName(userName);
        user.setName(name);
        ExampleMatcher matcher = ExampleMatcher.matching();
        if (userName != null) {
            matcher.withMatcher("userName", m -> m.exact());
        }
        if (name != null) {
            matcher.withMatcher("name", m -> m.exact());
        }
        Example<User> example = Example.of(user,matcher);
        Page<User> users = userRepository.findAll(example, page);
        return users;
    }

    public User getUserById(Long id){
        User user = userRepository.findById(id).get();
        return user;
    }

    public User findByUserName(String userName){
        return userRepository.findByUserName(userName);
    }
}
