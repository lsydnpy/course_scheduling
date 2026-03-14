package com.xy.course_scheduling.aop;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xy.course_scheduling.custom.annotations.OperLogAnn;
import com.xy.course_scheduling.entity.CustomUserDetails;
import com.xy.course_scheduling.entity.OperLog;
import com.xy.course_scheduling.service.OperLogService;
import com.xy.course_scheduling.utils.AdminUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

// 定义切面类，用于记录操作日志
@Aspect
@Component
public class OperLogAspect {

    // 创建 ObjectMapper 实例，用于 JSON 序列化
    private final ObjectMapper objectMapper;
    // 自动注入操作日志服务
    @Autowired
    private OperLogService operLogService;

    // 日志内容最大长度（避免存储过大数据）
    private static final int MAX_LOG_LENGTH = 2000;
    private static final int MAX_ERROR_LENGTH = 1000;

    public OperLogAspect() {
        this.objectMapper = new ObjectMapper();
        // 注册 Java 8 时间模块支持 LocalDateTime
        this.objectMapper.registerModule(new JavaTimeModule());
        // 禁用时间戳格式输出
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // 定义切点，匹配带有 OperLogAnn 注解的方法
    @Pointcut("@annotation(operLogAnn)")
    public void operLogPointCut(OperLogAnn operLogAnn) {
    }

    // 环绕通知，在目标方法执行前后进行操作日志记录
    @Around("operLogPointCut(operLogAnn)")
    public Object around(ProceedingJoinPoint point, OperLogAnn operLogAnn) throws Throwable {
        // 获取当前请求对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        // 创建操作日志对象
        OperLog log = new OperLog();
        // 设置日志标题
        log.setTitle(operLogAnn.title());
        // 设置业务类型
        log.setBusinessType(operLogAnn.businessType().name());
        // 设置方法签名
        log.setMethod(getMethodSignature(point));
        // 设置请求方法 (GET/POST 等)
        log.setRequestMethod(request.getMethod());
        // 设置操作 URL
        log.setOperUrl(request.getRequestURL().toString());
        // 设置操作者 IP 地址
        log.setOperIp(getIpAddress(request));
        // 设置请求参数（限制长度）
        log.setOperParam(truncateJson(getRequestParam(point), MAX_LOG_LENGTH));
        // 设置操作时间
        log.setOperTime(LocalDateTime.now());

        // 记录开始执行时间
        long startTime = System.currentTimeMillis();
        Object result = null;
        try {
            // 执行目标方法
            result = point.proceed();
            // 设置执行状态为成功
            log.setStatus(0); // 成功
            // 将返回结果序列化为 JSON 字符串（只存储摘要，限制长度）
            log.setJsonResult(truncateJson(buildResultSummary(point, result), MAX_LOG_LENGTH));
        } catch (Exception e) {
            // 设置执行状态为失败
            log.setStatus(1); // 失败
            // 记录错误信息（限制长度）
            log.setErrorMsg(truncateString(e.getMessage(), MAX_ERROR_LENGTH));
            // 重新抛出异常
            throw e;
        } finally {
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            log.setExecuteTime(endTime - startTime);
            // 获取当前登录用户信息
            CustomUserDetails details = AdminUtil.getCurrentAdmin();
            if (details != null) {
                // 设置操作者姓名
                log.setOperatorName(details.getName());
                // 设置操作者用户名
                log.setOperatorUsername(details.getUsername());
            }
            // 保存日志记录
            operLogService.save(log);
        }

        // 返回方法执行结果
        return result;
    }

    // 构建返回结果摘要（避免存储完整的大对象）
    private String buildResultSummary(ProceedingJoinPoint point, Object result) {
        if (result == null) {
            return "{\"code\":200,\"message\":\"操作成功\"}";
        }
        
        // 获取返回结果的类名
        String resultType = result.getClass().getSimpleName();
        
        // 如果是 Result 类型，只提取关键信息
        if (result instanceof com.xy.course_scheduling.entity.Result) {
            com.xy.course_scheduling.entity.Result<?> r = (com.xy.course_scheduling.entity.Result<?>) result;
            return String.format("{\"code\":%s,\"message\":\"%s\"}", 
                r.getCode(), 
                r.getMessage() != null ? r.getMessage() : "操作成功");
        }
        
        // 其他类型返回简要信息
        return String.format("{\"type\":\"%s\",\"success\":true}", resultType);
    }

    // 获取方法签名的辅助方法
    private String getMethodSignature(ProceedingJoinPoint point) {
        return point.getTarget().getClass().getName() + "." + point.getSignature().getName();
    }

    // 获取请求参数的辅助方法
    private String getRequestParam(ProceedingJoinPoint point) {
        Object[] args = point.getArgs();
        if (args != null && args.length > 0) {
            try {
                // 尝试将参数序列化为 JSON 字符串
                return objectMapper.writeValueAsString(args);
            } catch (JsonProcessingException e) {
                // 如果序列化失败，则返回第一个参数的字符串表示
                return args[0].toString();
            }
        }
        // 如果没有参数则返回空字符串
        return "";
    }

    // 获取 IP 地址的辅助方法，支持代理服务器获取真实 IP
    private String getIpAddress(HttpServletRequest request) {
        // 从 X-Forwarded-For 头获取 IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 如果 X-Forwarded-For 为空或 unknown，则从 Proxy-Client-IP 头获取
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 如果 Proxy-Client-IP 也为空或 unknown，则从 WL-Proxy-Client-IP 头获取
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 如果以上都获取不到，则使用远程地址
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    // 截断 JSON 字符串，避免存储过大数据
    private String truncateJson(String json, int maxLength) {
        if (json == null || json.length() <= maxLength) {
            return json;
        }
        return json.substring(0, maxLength) + "...[truncated]";
    }

    // 截断字符串，避免存储过大数据
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength) + "...[truncated]";
    }
}
