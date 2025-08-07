package io.github.benxincaomu.oa.bussiness.workflow;

import java.text.MessageFormat;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Service
public class DataTableRepository {

    @Resource
    private EntityManager entityManager;

    /**
     * 复制表结构
     * 
     * @param sourceTableName 源表名
     * @param targetTableName 目标表名
     */
    public void copyTable(String sourceTableName, String targetTableName) {
        String sql = "create table {0} ( LIKE {1} INCLUDING ALL INCLUDING COMMENTS);";
        entityManager.createNativeQuery(MessageFormat.format(sql, targetTableName, sourceTableName)).executeUpdate();
    }

    public long count(String tableName) {
        String sql = "SELECT last_value FROM pg_sequences WHERE sequencename = replace(pg_get_serial_sequence(:tableName, 'id'), 'public.', '')";
        Long count = (Long) entityManager.createNativeQuery(sql, Long.class).setParameter("tableName", tableName)
                .getSingleResult();

        return count == null ? 0 : count.longValue();
    }

    /**
     * 获取表名
     * 
     * @param tableNamePrefix
     * @return 表名列表
     */
    public List<String> getTableNamesLike(String tableNamePrefix) {
        String sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public' and tablename like :tableNamePrefix || '%'";
        List<String> resultList = entityManager.createNativeQuery(sql).setParameter("tableNamePrefix", tableNamePrefix)
                .getResultList();
        return resultList;
    }

  
}
