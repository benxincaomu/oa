package io.github.benxincaomu.oa.base.entity;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

import io.github.benxincaomu.oa.base.security.SaltedUser;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

public class TenantIdEntityListener {

    private final Logger logger = LoggerFactory.getLogger(TenantIdEntityListener.class);

    @PreUpdate
    @PrePersist
    public void touchForCreate(Object target) {
        // 获取所有字段（包括父类）
        List<Field> allFields = getAllFields(target.getClass());
        for (Field field : allFields) {
            if (field.isAnnotationPresent(TenantId.class)) {

                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof SaltedUser) {
                    SaltedUser user = (SaltedUser) principal;
                    try {
                        field.setAccessible(true);
                        String fieldName = field.getName();
                        String getterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                        Object value = target.getClass().getMethod(getterName).invoke(target);
                        if(value == null){
                            String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                            target.getClass().getMethod(setterName, field.getType()).invoke(target, user.getTenantId());
                        }
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException
                            | SecurityException e) {
                        logger.error("设置tenantId失败", e);
                    }
                }
            }
        }
    }
   
    

    /**
     * 递归获取类及其所有父类的字段
     */
    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                fields.add(field);
            }
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    
}
