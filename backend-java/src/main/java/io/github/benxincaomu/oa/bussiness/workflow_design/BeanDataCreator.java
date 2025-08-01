package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

@Service
public class BeanDataCreator {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Resource
    private EntityManager entityManager;

    public void createBeanDataTable() {
        
    }
}
