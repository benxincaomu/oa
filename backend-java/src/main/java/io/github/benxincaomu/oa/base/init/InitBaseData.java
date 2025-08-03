package io.github.benxincaomu.oa.base.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.bussiness.tenant.Tenant;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import io.github.benxincaomu.oa.bussiness.user.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

/**
 * 初始化基础数据
 */
@Service
public class InitBaseData {
    @Resource
    private TenantRepository tenantRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private UserService userService;


    private final Logger logger = LoggerFactory.getLogger(InitBaseData.class);
    // @PostConstruct
    public void init() {
        if (tenantRepository.count() == 0) {
            Tenant tenant = new Tenant();
            tenant.setTenantName("default");
            tenant.setEnable(true);
            tenantRepository.save(tenant);
        }
        if(userRepository.count()==0){
            User user = new User();
            user.setUserName("admin");
            String password = "123456";//StringGenerator.generate(8);
            user.setPassword(password);
            Sort sort = Sort.by(Sort.Direction.ASC, "id");
            PageRequest limit = PageRequest.of(0, 1,sort);
            ExampleMatcher matcher = ExampleMatcher.matching();
            matcher.withMatcher("enable",m->m.exact() );
            Tenant tenant = new Tenant();
            tenant.setEnable(true);
            Page<Tenant> tenants = tenantRepository.findAll(Example.of(tenant), limit);
            if (tenants.hasContent()){
                user.setTenantId(tenants.getContent().get(0).getId());
            }
            userService.insert(user);
            logger.info("user is \"admin\" and password is \"{}\"",password);
        }
    }
}
