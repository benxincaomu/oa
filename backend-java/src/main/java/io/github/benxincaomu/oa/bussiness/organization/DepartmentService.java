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

import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import jakarta.annotation.Resource;

@Service
public class DepartmentService {

    @Resource
    private DepartmentRepository departmentRepository;

    /**
     * 新增部门
     * @param department
     * @return 1成功
     */
    @Transactional
    public int insert(Department department){
        Asserts.isTrue(departmentRepository.existsByName(department.getName()), OaResponseCode.DEPT_NAME_EXITS);
        return departmentRepository.save(department).getId() == null?0 :1;
    }
    @Transactional
    public int update(Department department){
        return departmentRepository.save(department).getId() == null?0 :1;
    }

    public Page<Department> list(String name,String code,Integer currPage,Integer pageSize){
        currPage = currPage == null || currPage < 0 ? 0 : currPage-1;
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

    public List<DeptVo> findDeptTree() { 

        return null;
    }
    List<DeptVo> findDeptByParentId(Long parentId){
        List<DeptVo> depts = departmentRepository.findAllByParentId(parentId);
        if (!CollectionUtils.isEmpty(depts)) {
            for (DeptVo dept : depts) {
                dept.setChildren(findDeptByParentId(dept.getId()));
            }
        }
        return depts;

    }
}
