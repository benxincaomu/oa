package io.github.benxincaomu.oa.base.web;

import io.github.benxincaomu.notry.code.ResponseCode;

public enum OaResponseCode implements ResponseCode {
	TOKEN_NOT_EXIST(10002,"token不存在"),
	NO_PERMISSION(403,"无权限"),
    USER_NAME_EXITS(10001,"用户名已存在"),
    EMAIL_EXITS(10001,"邮箱已被使用"),
	USER_NOT_EXIST(10003,"用户不存在"),
	UPDATE_PASSWORD_EXPIRED(10004,"修改密码超时"),
	DEFAULT_ROLE_CAN_NOT_AUTH(10005,"默认角色不能更改授权"),
	DEPT_NAME_EXITS(10006,"部门名称已存在"),

	WORKBENCH_PUBLISH_NOT_EXIST(20001,"流程未发布"),
	WORKBENCH_NOT_EXIST(20002,"工作台不存在"),
	FLOW_TASK_NOT_EXIST(20003,"流程任务已处理或无权限"),
	PROJECT_NOT_INITIALIZED(-1,"项目未初始化"),
    ;
    

	private int code;
	private String message;

	private OaResponseCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getMessage() {
		return message;
	}
}
