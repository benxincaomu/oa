package io.github.benxincaomu.oa.bussiness.workflow_design;


import io.github.benxincaomu.oa.base.utils.Pair;


public class Consts {
    // public final static Map<String,String> columnTypes = new LinkedHashMap<>();

    /** 列类型 */
    public final static Pair[] COLUMN_TYPES = {
        new Pair("number", "数字"),
        new Pair("string", "字符串"),
        new Pair("longtext", "长文本"),
        /* new Pair("file", "文件"),
        new Pair("image", "图片"),
        new Pair("boolean", "布尔值"),
        new Pair("number[]", "数字数组"),
        new Pair("boolean[]", "布尔值数组"),
        new Pair("string[]", "字符串数组"),
        new Pair("object", "复合对象"),
        new Pair("object[]", "复合对象数组"), */
    };
    /** 列验证类型 */
    public final static Pair[] NUMBER_VALIDATE_TYPES = {
        new Pair("required", "必填"),        
        new Pair("integer", "整数"),
        new Pair("range", "范围"),
    };
    public final static Pair[] STRING_VALIDATE_TYPES = {
        new Pair("required", "必填"),
        new Pair("email", "邮箱"),
        new Pair("phone", "手机"),
        new Pair("url", "网址"),
        new Pair("regex", "正则"),
    };

    /** 任务分配类型 */
    public static final Pair[] ASSIGNEE_TYPE = {

        new Pair("user", "指定用户"),
        new Pair("dept", "部门"),
        new Pair("role", "发起人部门负责人"),
    };

    
    
}
