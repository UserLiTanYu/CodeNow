package com.codenow.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * 上传文件安全校验器。扩展名和 MIME 仅用于初筛，最终通过 Magic Bytes 检查真实类型，
 * 防止客户端伪造 Content-Type 或用危险内容冒充图片。
 */
public final class FileValidator {

    /**
     * 私有构造方法，防止实例化工具类。
     */
    private FileValidator() {
    }

    /** 允许上传的图片文件扩展名白名单 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 校验文件是否为合法的图片文件。
     * <p>
     * 校验流程：先检查扩展名是否在白名单内，再读取文件头的 Magic Bytes 验证真实文件类型，
     * 防止客户端伪造扩展名或 Content-Type。
     * </p>
     *
     * @param inputStream    文件输入流（调用方负责关闭）
     * @param originalFilename 原始文件名（用于获取扩展名）
     * @return 校验通过的文件扩展名（小写）
     * @throws IOException            如果读取文件头失败
     * @throws IllegalArgumentException 如果文件为空、扩展名不合法或文件内容与扩展名不匹配
     */
    public static String validateImage(InputStream inputStream, String originalFilename) throws IOException {
        if (inputStream == null || originalFilename == null) {
            throw new IllegalArgumentException("文件不能为空");
        }

        // 1. 校验扩展名
        String ext = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、GIF、WebP 格式的图片");
        }

        // 2. 读取文件头字节（最多读 12 字节，WebP 需要 12 字节判断）
        byte[] header = new byte[12];
        int bytesRead = inputStream.read(header);
        if (bytesRead < 4) {
            throw new IllegalArgumentException("文件内容不完整或不是有效的图片文件");
        }

        // 3. 通过 Magic Bytes 验证文件真实类型
        if (!isValidImage(header, ext)) {
            throw new IllegalArgumentException("文件扩展名与实际内容不匹配，请上传真实的图片文件");
        }

        return ext;
    }

    /**
     * 通过 Magic Bytes 判断文件是否为合法图片。
     * <p>
     * 根据文件扩展名读取文件头字节，与已知的图片格式魔数进行比对。
     * 支持 JPG、PNG、GIF、WebP 四种格式。
     * </p>
     *
     * @param header 文件头字节数组（至少12字节）
     * @param ext    文件扩展名（小写）
     * @return 如果文件头与扩展名匹配则返回 {@code true}，否则返回 {@code false}
     */
    private static boolean isValidImage(byte[] header, String ext) {
        // JPG: FF D8 FF
        if (Set.of("jpg", "jpeg").contains(ext)) {
            return header.length >= 3
                    && header[0] == (byte) 0xFF
                    && header[1] == (byte) 0xD8
                    && header[2] == (byte) 0xFF;
        }

        // PNG: 89 50 4E 47
        if ("png".equals(ext)) {
            return header.length >= 4
                    && header[0] == (byte) 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47;
        }

        // GIF: 47 49 46 38 (GIF8)
        if ("gif".equals(ext)) {
            return header.length >= 4
                    && header[0] == 0x47
                    && header[1] == 0x49
                    && header[2] == 0x46
                    && header[3] == 0x38;
        }

        // WebP: RIFF....WEBP (前 4 字节为 RIFF，第 8-11 字节为 WEBP)
        if ("webp".equals(ext)) {
            return header.length >= 12
                    && header[0] == 0x52  // R
                    && header[1] == 0x49  // I
                    && header[2] == 0x46  // F
                    && header[3] == 0x46  // F
                    && header[8] == 0x57  // W
                    && header[9] == 0x45  // E
                    && header[10] == 0x42 // B
                    && header[11] == 0x50; // P
        }

        return false;
    }

    /**
     * 获取文件扩展名。
     *
     * @param filename 文件名
     * @return 文件扩展名（不包含"."），如果文件名为空或不含扩展名则返回空字符串
     */
    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}
