package io.github.benxincaomu.oa.perfomacevs;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("oa")
@EnableJpaRepositories(basePackages = { "io.github.benxincaomu.oa.perfomacevs" },includeFilters = {@Filter(type = FilterType.ANNOTATION, classes = { Repository.class})})
public class JpaVsMybatisTest {

}
