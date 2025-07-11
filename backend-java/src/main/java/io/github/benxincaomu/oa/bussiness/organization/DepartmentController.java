package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/organize")
public class DepartmentController {

    private final DepartmentService departmentService;

    DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * 部门列表
     * 
     * @param name     部门名称
     * @param code     部门代码
     * @param currPage 当前页
     * @param pageSize 分页大小
     * @return
     */
    @GetMapping("list")
    public PagedModel<Department> list(String name, String code, Integer currPage, Integer pageSize) {
        return new PagedModel<>(departmentService.list(name, code, currPage, pageSize));
    }

    @PostMapping
    public void insert(@RequestBody Department department) {
        departmentService.insert(department);
    }

    @GetMapping("/{id}")
    public Department getById(@PathParam("id") Long id) {
        return departmentService.getById(id);

    }

    @GetMapping("/listTree")
    public List<DeptVo> listTree() {
        return departmentService.findAllDeptAsTree();
    }

    @GetMapping("/listAll")
    public List<DeptVo> listAll() {
        return departmentService.listAll();
    }

}
