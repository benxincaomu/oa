package io.github.benxincaomu.oa.bussiness.organization.vo;

import jakarta.validation.constraints.NotNull;

public class DeptLeaderVo {
    @NotNull(message = "部门不能为空")
    private Long deptId;
    @NotNull(message = "部门领导不能为空")
    private Long leaderUserId;
    public Long getDeptId() {
        return deptId;
    }
    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }
    public Long getLeaderUserId() {
        return leaderUserId;
    }
    public void setLeaderUserId(Long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }

    
}
