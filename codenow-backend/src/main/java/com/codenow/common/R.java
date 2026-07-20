package com.codenow.common;

import lombok.Data;



/**
 * 统一响应结果包装类，用于封装所有接口的返回数据。
 * <p>
 * 泛型 {@code T} 表示响应数据的具体类型，通过静态工厂方法 {@link #ok(Object)} 和
 * {@link #error(String)} 快速构建成功或失败的响应。
 * </p>
 *
 * @param <T> 响应数据的类型
 */
@Data
public class R<T> {

    /** 响应状态码，200 表示成功，500 表示服务器错误 */
    private Integer code;

    /** 响应提示信息 */
    private String message;

    /** 响应数据载体 */
    private T data;

    /**
     * 构建成功的响应结果，携带数据。
     *
     * @param <T>  响应数据的类型
     * @param data 响应数据
     * @return 包含成功状态码（200）和数据的响应对象
     */
    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        return r;
    }

    /**
     * 构建成功的响应结果，不携带数据。
     *
     * @param <T> 响应数据的类型
     * @return 包含成功状态码（200）的响应对象
     */
    public static <T> R<T> ok() {
        return ok(null);
    }

    /**
     * 构建失败的响应结果，使用默认错误码 500。
     *
     * @param <T>     响应数据的类型
     * @param message 错误提示信息
     * @return 包含错误状态码（500）和错误信息的响应对象
     */
    public static <T> R<T> error(String message) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    /**
     * 构建失败的响应结果，使用自定义错误码。
     *
     * @param <T>     响应数据的类型
     * @param code    自定义错误状态码
     * @param message 错误提示信息
     * @return 包含自定义错误状态码和错误信息的响应对象
     */
    public static <T> R<T> error(Integer code, String message) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }
}
