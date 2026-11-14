package org.example.wxbot.util;

import java.io.IOException;
import java.io.InputStream;

public final class ImageUtil {

    private ImageUtil() {
    }

    public static byte[] toBytes(InputStream input) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("图片输入流不能为空");
        }
        try (input) {
            return input.readAllBytes();
        }
    }

    public static byte[] fromClasspath(String path) {
        try (InputStream input = ImageUtil.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("缺少图片资源: " + path);
            }
            return toBytes(input);
        } catch (IOException exception) {
            throw new IllegalStateException("读取图片资源失败: " + path, exception);
        }
    }
}
