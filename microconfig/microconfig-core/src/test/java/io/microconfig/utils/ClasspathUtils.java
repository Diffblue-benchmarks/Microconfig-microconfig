package io.microconfig.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public class ClasspathUtils {
    public static File getClasspathFile(String name) {
        try {
            return new ClassPathResource(name).getFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(String file) {
        try {
            return IoUtils.readFully(new ClassPathResource(file).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}