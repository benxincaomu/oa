package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import io.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.annotation.Resource;

@Service
public class DepartmentService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private DepartmentUserRepository departmentUserRepository;

    /**
     * 新增部门
     * @param department
     * @return 1成功
     */
    @Transactional
    public int insert(Department department){
        Asserts.isFalse(departmentRepository.existsByName(department.getName()), OaResponseCode.DEPT_NAME_EXITS);
        return departmentRepository.save(department).getId() == null?0 :1;
    }
    @Transactional
    public int update(Department department){
        Asserts.isFalse(departmentRepository.existsByNameAndIdNot(department.getName(), department.getId()), OaResponseCode.DEPT_NAME_EXITS);
        
        if (department.getParentId() != null) {
            Long deptId = department.getId();
            Long parentId = department.getParentId();
            
            Asserts.isFalse(deptId.equals(parentId), OaResponseCode.DEPT_PARENT_INVALID);
            
            List<Long> descendantIds = getAllDescendantIds(deptId);
            Asserts.isFalse(descendantIds.contains(parentId), OaResponseCode.DEPT_PARENT_INVALID);
        }
        
        return departmentRepository.save(department).getId() == null?0 :1;
    }
    
    private List<Long> getAllDescendantIds(Long parentId) {
        List<Long> result = new java.util.ArrayList<>();
        List<Department> children = departmentRepository.findAllByParentIdDept(parentId, JpaAuditorAware.getCurrentTenantId());
        for (Department child : children) {
            result.add(child.getId());
            result.addAll(getAllDescendantIds(child.getId()));
        }
        return result;
    }

    public Page<Department> list(String name,String code,Integer currPage,Integer pageSize){
        currPage = currPage == null || currPage <= 0 ? 0 : currPage-1;
        pageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        PageRequest page = PageRequest.of(currPage, pageSize, Sort.by("createAt"));
        ExampleMatcher matcher = ExampleMatcher.matching();
        Department department = new Department();
        if (name != null && !name.isEmpty()) {
            matcher.withMatcher("name", m -> m.exact());
            department.setName(name);
        }
        if (code != null && !code.isEmpty()) {
            matcher.withMatcher("code", m -> m.exact());
            department.setCode(code);
        }
        Example<Department> example = Example.of(department, matcher);
        Page<Department> departments = departmentRepository.findAll(example, page);
        return departments;

    }

 /*    public List<DeptVo> findDeptTree() { 
        List<DeptVo> deptVos = departmentRepository.findAllByParentId(null,JpaAuditorAware.getCurrentTenantId());
        return deptVos;
    }
    List<DeptVo> findDeptByParentId(Long parentId){
        List<DeptVo> depts = departmentRepository.findAllByParentId(parentId,JpaAuditorAware.getCurrentTenantId());
        if (!CollectionUtils.isEmpty(depts)) {
            for (DeptVo dept : depts) {
                dept.setChildren(findDeptByParentId(dept.getId()));
            }
        }
        return depts;
    }   public List<DeptVo> findDeptTree() { 
        List<DeptVo> deptVos = departmentRepository.findAllByParentId(null,JpaAuditorAware.getCurrentTenantId());
        return deptVos;
    }
    List<DeptVo> findDeptByParentId(Long parentId){
        List<DeptVo> depts = departmentRepository.findAllByParentId(parentId,JpaAuditorAware.getCurrentTenantId());
        if (!CollectionUtils.isEmpty(depts)) {
            for (DeptVo dept : depts) {
                dept.setChildren(findDeptByParentId(dept.getId()));
            }
        }
        return depts;
    } */
    @Transactional
    public List<DeptVo> findAllDeptAsTree() {
        List<DeptVo> depts = findAllDeptAsTree(null);
        return depts;
    }

    List<DeptVo> findAllDeptAsTree(Long parentId) {
        List<DeptVo> depts = departmentRepository.findAllByParentId(parentId,JpaAuditorAware.getCurrentTenantId());
        if (!CollectionUtils.isEmpty(depts)) {
            for (DeptVo dept : depts) {
                dept.setChildren(findAllDeptAsTree(dept.getId()));
            }
        }
        return depts;
    }

    public Department getById(Long id) {
        return departmentRepository.findById(id).orElse(null);
    }
    public List<DeptVo> listAll() {
        return departmentRepository.listAll();
    }
    @Transactional
    public void updateLeader(Department department) {
        departmentRepository.updateLeader(department);
    }
    public List<UserIdNameVo> getUsersByDeptId(Long deptId) {
        return departmentUserRepository.getUsersByDeptId(deptId);
    }
}
