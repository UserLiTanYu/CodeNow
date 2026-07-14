package com.codenow.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /**
     * 上传文件，返回可访问的 URL
     */
    String upload(MultipartFile file);

    /**
     * 删除文件
     */
    void delete(String url);
}
