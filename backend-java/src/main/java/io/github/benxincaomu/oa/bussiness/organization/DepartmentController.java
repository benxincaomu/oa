package io.github.benxincaomu.oa.bussiness.organization;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("department")
public class DepartmentController {

    private final DepartmentService departmentService;

    DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 部门列表
     * @param name 部门名称
     * @param code 部门代码
     * @param currPage 当前页
     * @param pageSize 分页大小
     * @return
     */
    @GetMapping("list")
    public PagedModel<Department> list(String name,String code,Integer currPage,Integer pageSize){
        return new PagedModel<>(departmentService.list(name, code, currPage, pageSize));
    }


    @PostMapping
    public void insert(Department department){
        departmentService.insert(department);
    }


}
