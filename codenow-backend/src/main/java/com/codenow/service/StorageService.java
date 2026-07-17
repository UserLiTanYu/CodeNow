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
     * 删除文件
     */
    void delete(String url);
}
