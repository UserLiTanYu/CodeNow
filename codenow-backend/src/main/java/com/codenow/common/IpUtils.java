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
     * 获取真实客户端 IP 地址（限流专用）。
     * <p>
     * 直接使用 {@code request.getRemoteAddr()} 获取 TCP 连接的来源 IP，
     * 不信任客户端可能伪造的 X-Forwarded-For 头，确保限流计数的准确性。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 真实客户端 IP 地址
     */
    public static String getRealIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    /**
     * 获取代理后的客户端 IP 地址（日志专用）。
     * <p>
     * 优先从 X-Forwarded-For 头获取（经过可信 Nginx 代理时有效），
     * 多个代理时取第一个 IP；其次尝试 X-Real-IP 头；最后回退到 {@code getRemoteAddr()}。
     * </p>
     *
     * @param request HTTP 请求对象
     * @return 代理后的客户端 IP 地址
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
