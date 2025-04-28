package dev.vality.testcontainers.annotations.clickhouse.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtil {

    public static String getFile(String fileName) {
        var classLoader = FileUtil.class.getClassLoader();
        try {
            return IOUtils.toString(classLoader.getResourceAsStream(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error when getFile e: ", e);
            return "";
        }
    }
}
