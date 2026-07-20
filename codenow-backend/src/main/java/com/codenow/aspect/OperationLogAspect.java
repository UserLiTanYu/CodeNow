package com.codenow.aspect;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.annotation.OperationLog;
import com.codenow.common.IpUtils;
import com.codenow.entity.SysOperationLog;
import com.codenow.service.OperationLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作审计切面。业务成功或失败都会记录耗时和状态；日志写入失败不得覆盖原业务结果。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /** 操作日志服务，负责异步持久化日志记录 */
    private final OperationLogService operationLogService;

    /** JSON 序列化工具，用于将请求参数序列化为字符串 */
    private final ObjectMapper objectMapper;

    /**
     * 环绕通知：拦截标注了 {@link OperationLog} 注解的方法，
     * 记录方法执行耗时与成功/失败状态，最终异步写入操作日志。
     *
     * @param joinPoint 连接点，包含被拦截方法的信息
     * @return 被拦截方法的执行结果
     * @throws Throwable 业务方法抛出的异常（原样透传，不影响异常语义）
     */
    @Around("@annotation(com.codenow.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 先执行业务方法
        Object result;
        int status = 1;
        try {
            result = joinPoint.proceed();
        } catch (Throwable e) {
            status = 0;
            throw e;
        } finally {
            // finally 确保异常请求同样留下审计记录；记录失败不能改变原业务异常语义。
            long duration = System.currentTimeMillis() - startTime;
            // 异步记录日志
            try {
                saveLog(joinPoint, status, duration);
            } catch (Exception e) {
                log.warn("记录操作日志失败: {}", e.getMessage());
            }
        }
        return result;
    }

    /**
     * 组装操作日志实体并异步保存。收集的信息包括：操作描述、方法全限定名、
     * 请求参数（自动截断超长内容）、客户端 IP、操作人身份（Sa-Token）。
     *
     * @param joinPoint 连接点，用于获取方法签名和参数
     * @param status    执行状态，1-成功，0-失败
     * @param duration  方法执行耗时（毫秒）
     */
    private void saveLog(ProceedingJoinPoint joinPoint, int status, long duration) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        OperationLog annotation = method.getAnnotation(OperationLog.class);

        SysOperationLog logEntity = new SysOperationLog();

        // 操作描述
        logEntity.setOperation(annotation.value());

        // 方法名（类名.方法名）
        String className = joinPoint.getTarget().getClass().getSimpleName();
        logEntity.setMethod(className + "." + method.getName());

        // 请求参数（排除 HttpServletRequest 等不可序列化的参数）
        try {
            Object[] args = joinPoint.getArgs();
            Object[] serializableArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof HttpServletRequest) {
                    serializableArgs[i] = "{HttpServletRequest}";
                } else {
                    serializableArgs[i] = args[i];
                }
            }
            String params = objectMapper.writeValueAsString(serializableArgs);
            // 截断过长的参数
            if (params.length() > 2000) {
                params = params.substring(0, 2000) + "...";
            }
            logEntity.setParams(params);
        } catch (Exception e) {
            logEntity.setParams("{}");
        }

        // IP 地址
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logEntity.setIp(IpUtils.getProxyIp(request));
            }
        } catch (Exception e) {
            logEntity.setIp("unknown");
        }

        // 操作人信息（从 Sa-Token 获取）
        try {
            if (StpUtil.isLogin()) {
                logEntity.setUserId(StpUtil.getLoginIdAsLong());
                // username 从 token 的 session 中获取
                Object username = StpUtil.getSession().get("username");
                logEntity.setUsername(username != null ? username.toString() : "unknown");
            }
        } catch (Exception e) {
            // 未登录时（如登录接口本身），userId 和 username 为空
        }

        logEntity.setDuration((int) duration);
        logEntity.setStatus(status);
        logEntity.setCreateTime(LocalDateTime.now());

        // 异步写入数据库
        operationLogService.saveAsync(logEntity);
    }
}
