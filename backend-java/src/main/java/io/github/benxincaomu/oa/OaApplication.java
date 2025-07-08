package io.github.benxincaomu.oa;

import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@EnableJpaAuditing
@SpringBootApplication
@EnableJpaRepositories(basePackages = { "io.github.benxincaomu.oa.bussiness" })
@EnableRedisRepositories(basePackages = "io.github.benxincaomu.oa.base")
@ComponentScan({ "com.github.benxincaomu.notry", "io.github.benxincaomu.oa" })
public class OaApplication {
	private static final Logger logger = LoggerFactory.getLogger(OaApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(OaApplication.class, args);

	}

}
