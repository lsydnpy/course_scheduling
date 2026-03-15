package com.xy.course_scheduling.custom.annotations;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * <p>
 * 用于标记需要记录操作日志的方法，配合 AOP 切面自动记录系统操作日志。
 * 当 Controller 方法被此注解标记后，系统会自动将操作信息记录到 oper_log 表中。
 * </p>
 * 
 * @author ${USER}
 * @since 1.0.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLogAnn {
    /**
     * 操作模块名称
     * <p>用于标识操作所属的功能模块，如"用户管理"、"课程管理"、"教师管理"等。</p>
     * <p>在日志列表中显示，便于后续查询和统计。</p>
     *
     * @return 模块名称，默认为空字符串
     */
    String title() default "";

    /**
     * 操作类型
     * <p>用于标识具体业务操作类型，包括新增、修改、删除、查询和其他操作。</p>
     * <p>AOP 切面会根据此类型记录不同的操作行为。</p>
     *
     * @return 业务操作类型，默认为 OTHER（其他操作）
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 业务操作类型枚举
     * <p>定义系统中常见的操作类型，用于分类记录操作日志。</p>
     */
    enum BusinessType {
        /** 新增操作 */
        INSERT,
        /** 修改操作 */
        UPDATE,
        /** 删除操作 */
        DELETE,
        /** 查询操作 */
        SELECT,
        /** 其他操作（如导入、导出、审核等） */
        OTHER
    }
}
