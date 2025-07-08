package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.EnumSet;
import java.util.HashMap;

import org.hibernate.Session;
import org.hibernate.boot.Metadata;
import org.hibernate.relational.SchemaManager;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.tool.schema.spi.SchemaManagementTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

@Service
public class BeanDataCreator {
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    public void createBeanDataTable() {

        

    }
}
