package com.codenow.service;

import com.codenow.common.FileValidator;
import com.codenow.dto.ArticlePackageVO;
import com.codenow.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ArticlePackageImportService {
    public static final long MAX_ZIP_SIZE = 25L * 1024 * 1024;
    private static final long MAX_MARKDOWN_SIZE = 2L * 1024 * 1024;
    private static final long MAX_ENTRY_SIZE = 5L * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_SIZE = 50L * 1024 * 1024;
    private static final int MAX_ENTRIES = 100;
    private static final Charset GBK = Charset.forName("GBK");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "!\\[([^\\]]*)]\\((?:<([^>]+)>|([^\\s)]+))(\\s+(?:\"[^\"]*\"|'[^']*'|\\([^)]*\\)))?\\)");
    private static final Pattern H1_PATTERN = Pattern.compile("(?m)^\\s*#\\s+(.+?)\\s*#*\\s*$");

    private final StorageService storageService;

    public ArticlePackageVO importPackage(MultipartFile file) {
        validateZip(file);
        PackageContent packageContent = readPackage(file);
        List<String> uploadedUrls = new ArrayList<>();
        try {
            RewriteResult rewrite = rewriteImages(packageContent, uploadedUrls);
            return new ArticlePackageVO(extractTitle(packageContent.markdownPath, rewrite.content),
                    rewrite.content, rewrite.imageCount);
        } catch (RuntimeException e) {
            uploadedUrls.forEach(storageService::delete);
            throw e;
        }
    }

    private void validateZip(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(400, "请选择 ZIP 文章包");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new BusinessException(400, "仅支持 .zip 文章包");
        }
        if (file.getSize() > MAX_ZIP_SIZE) throw new BusinessException(400, "ZIP 文章包不能超过 25MB");
        try (var input = file.getInputStream()) {
            byte[] header = input.readNBytes(4);
            boolean zipHeader = header.length == 4 && header[0] == 0x50 && header[1] == 0x4B
                    && ((header[2] == 0x03 && header[3] == 0x04)
                    || (header[2] == 0x05 && header[3] == 0x06)
                    || (header[2] == 0x07 && header[3] == 0x08));
            if (!zipHeader) throw new BusinessException(400, "文件不是有效的 ZIP 文章包");
        } catch (IOException e) {
            throw new BusinessException(400, "ZIP 文章包读取失败");
        }
    }

    private PackageContent readPackage(MultipartFile file) {
        try {
            return readPackage(file, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            if (!isFilenameDecodingError(e)) {
                throw new BusinessException(400, "ZIP 文章包损坏或路径不合法");
            }
            try {
                return readPackage(file, GBK);
            } catch (IllegalArgumentException retryError) {
                throw new BusinessException(400, "ZIP 文章包损坏或路径不合法");
            }
        }
    }

    private PackageContent readPackage(MultipartFile file, Charset filenameCharset) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        String markdownPath = null;
        long totalSize = 0;
        int entryCount = 0;
        try (ZipInputStream zip = new ZipInputStream(file.getInputStream(), filenameCharset)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entryCount > MAX_ENTRIES) throw new BusinessException(400, "ZIP 内文件数量不能超过 100 个");
                if (entry.isDirectory()) continue;
                String path = normalizeEntryPath(entry.getName());
                if (path.startsWith("__MACOSX/") || path.endsWith("/.DS_Store") || path.equals(".DS_Store")) continue;
                String extension = extension(path);
                long entryLimit = "md".equals(extension) ? MAX_MARKDOWN_SIZE : MAX_ENTRY_SIZE;
                byte[] content = readEntry(zip, entryLimit);
                totalSize += content.length;
                if (totalSize > MAX_UNCOMPRESSED_SIZE) {
                    throw new BusinessException(400, "ZIP 解压后的总大小不能超过 50MB");
                }
                if ("md".equals(extension)) {
                    if (markdownPath != null) throw new BusinessException(400, "ZIP 中只能包含一个 Markdown 文件");
                    markdownPath = path;
                    files.put(path, content);
                } else if (IMAGE_EXTENSIONS.contains(extension)) {
                    files.put(path, content);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            throw new BusinessException(400, "ZIP 文章包损坏或路径不合法");
        }
        if (markdownPath == null) throw new BusinessException(400, "ZIP 中未找到 Markdown 文件");
        return new PackageContent(markdownPath, files, decodeUtf8(files.get(markdownPath)));
    }

    private boolean isFilenameDecodingError(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof CharacterCodingException) return true;
            current = current.getCause();
        }
        return false;
    }

    private RewriteResult rewriteImages(PackageContent packageContent, List<String> uploadedUrls) {
        Matcher matcher = IMAGE_PATTERN.matcher(packageContent.markdown);
        StringBuffer output = new StringBuffer();
        Map<String, String> uploadedByPath = new HashMap<>();
        int imageCount = 0;
        while (matcher.find()) {
            String target = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (isRemoteTarget(target)) {
                matcher.appendReplacement(output, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            String imagePath = resolveImagePath(packageContent.markdownPath, target);
            byte[] image = packageContent.files.get(imagePath);
            if (image == null) throw new BusinessException(400, "Markdown 引用的图片不存在：" + target);
            String url = uploadedByPath.get(imagePath);
            if (url == null) {
                validateImage(imagePath, image);
                url = storageService.upload(filename(imagePath), image);
                uploadedByPath.put(imagePath, url);
                uploadedUrls.add(url);
                imageCount++;
            }
            String title = matcher.group(4) == null ? "" : matcher.group(4);
            String replacement = "![" + matcher.group(1) + "](" + url + title + ")";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return new RewriteResult(output.toString(), imageCount);
    }

    private void validateImage(String path, byte[] content) {
        try {
            FileValidator.validateImage(new ByteArrayInputStream(content), filename(path));
        } catch (IOException | IllegalArgumentException e) {
            throw new BusinessException(400, "图片文件不合法：" + path);
        }
    }

    private byte[] readEntry(ZipInputStream zip, long limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        long size = 0;
        while ((read = zip.read(buffer)) != -1) {
            size += read;
            if (size > limit) throw new BusinessException(400, "ZIP 中单个文件大小超过限制");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private String normalizeEntryPath(String raw) {
        String path = raw.replace('\\', '/');
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") || path.indexOf('\0') >= 0
                || Arrays.asList(path.split("/")).contains("..")) {
            throw new IllegalArgumentException("unsafe path");
        }
        return normalizePath(path);
    }

    private String resolveImagePath(String markdownPath, String rawTarget) {
        String target = rawTarget.split("[?#]", 2)[0].replace('\\', '/');
        try {
            target = URLDecoder.decode(target.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Markdown 图片路径编码不正确：" + rawTarget);
        }
        int slash = markdownPath.lastIndexOf('/');
        String base = slash < 0 ? "" : markdownPath.substring(0, slash + 1);
        try {
            return normalizePath(base + target);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "Markdown 图片路径超出文章包范围：" + rawTarget);
        }
    }

    private String normalizePath(String raw) {
        Deque<String> parts = new ArrayDeque<>();
        for (String part : raw.split("/")) {
            if (part.isEmpty() || ".".equals(part)) continue;
            if ("..".equals(part)) {
                if (parts.isEmpty()) throw new IllegalArgumentException("outside root");
                parts.removeLast();
            } else {
                parts.addLast(part);
            }
        }
        if (parts.isEmpty()) throw new IllegalArgumentException("empty path");
        return String.join("/", parts);
    }

    private String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString().replaceFirst("^\\uFEFF", "");
        } catch (CharacterCodingException e) {
            throw new BusinessException(400, "Markdown 文件必须使用 UTF-8 编码");
        }
    }

    private String extractTitle(String markdownPath, String markdown) {
        Matcher heading = H1_PATTERN.matcher(markdown);
        if (heading.find()) return heading.group(1).trim();
        String name = filename(markdownPath);
        return name.substring(0, name.length() - 3);
    }

    private boolean isRemoteTarget(String target) {
        String lower = target.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("data:")
                || lower.startsWith("//") || lower.startsWith("#");
    }

    private String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String filename(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }

    private record PackageContent(String markdownPath, Map<String, byte[]> files, String markdown) {}
    private record RewriteResult(String content, int imageCount) {}
}
