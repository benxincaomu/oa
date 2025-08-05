package io.github.benxincaomu.oa.workflow;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benxincaomu.oa.bussiness.workflow.FlowForm;
import io.github.benxincaomu.oa.bussiness.workflow.FlowFormRepository;
import jakarta.annotation.Resource;

@SpringBootTest
public class FlowFormTest {
    @Resource
    private FlowFormRepository flowFormRepository;

    @Resource
    private ObjectMapper mapper;

    private final Logger logger = LoggerFactory.getLogger(FlowFormTest.class);

    @Test
    public void testFindOneById() throws JsonProcessingException{
        FlowForm flowForm = flowFormRepository.findOneById(1L,"flow_form_table_0");
        logger.info(mapper.writeValueAsString(flowForm));
    }

  
}
