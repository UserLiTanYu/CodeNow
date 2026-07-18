package com.codenow.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * 上传文件，返回可访问的 URL
     */
    String upload(MultipartFile file);

    /**
     * 上传内存中的文件内容，供文章包导入等服务端生成场景使用。
     */
    String upload(String originalFilename, byte[] content);

    /**
     * 判断 URL 是否由当前存储实现签发，避免业务字段接受任意第三方资源地址。
     */
    boolean isManagedUrl(String url);

    /**
     * 删除文件
     */
    void delete(String url);
}
