package com.codenow.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP 地址获取工具类
 * <p>
 * 限流场景：直接使用 getRemoteAddr()，不信任客户端伪造的 X-Forwarded-For
 * 日志场景：优先使用 X-Forwarded-For（经过可信 Nginx 代理时有效）
 */
public final class IpUtils {

    private IpUtils() {
    }

    /**
     * 获取真实客户端 IP（限流专用，不信任 X-Forwarded-For）
     */
    public static String getRealIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * 获取代理 IP（日志专用，信任 Nginx 传递的 X-Forwarded-For）
     */
    public static String getProxyIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多个代理时取第一个
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
