package com.codenow.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口
 */
public interface StorageService {

    /**
     * 上传文件，返回可访问的 URL
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    String upload(MultipartFile file);

    /**
     * 上传内存中的文件内容，供文章包导入等服务端生成场景使用
     *
     * @param originalFilename 原始文件名
     * @param content          文件内容字节数组
     * @return 文件访问URL
     */
    String upload(String originalFilename, byte[] content);

    /**
     * 判断 URL 是否由当前存储实现签发，避免业务字段接受任意第三方资源地址
     *
     * @param url 文件URL
     * @return 如果是当前存储实现签发的URL则返回 true
     */
    boolean isManagedUrl(String url);

    /**
     * 删除文件
     *
     * @param url 文件访问URL
     */
    void delete(String url);
}
