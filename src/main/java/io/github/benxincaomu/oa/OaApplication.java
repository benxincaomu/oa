package io.github.benxincaomu.oa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.base.security.TokenValueRepository;

@EnableJpaAuditing
@SpringBootApplication
// bussiness 包下用 @Repository 注解登记；base.security 只有 KeycloakObjectLinkRepository 是 JPA 仓库，
// 用 JpaRepository 可赋值类型过滤器把它纳入扫描（TokenValueRepository 属于 Redis，显式排除）。
@EnableJpaRepositories(
        basePackages = { "io.github.benxincaomu.oa.bussiness", "io.github.benxincaomu.oa.base.security" },
        includeFilters = {
                @Filter(type = FilterType.ANNOTATION, classes = { Repository.class }),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { JpaRepository.class })
        },
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { TokenValueRepository.class }))
@EnableRedisRepositories(basePackages = "io.github.benxincaomu.oa.base")
@ComponentScan({ "io.github.benxincaomu.notry", "io.github.benxincaomu.oa" })
@EnableConfigurationProperties
public class OaApplication {
	private static final Logger logger = LoggerFactory.getLogger(OaApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(OaApplication.class, args);

	}

}
