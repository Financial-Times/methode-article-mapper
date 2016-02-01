package com.ft.common;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by julia.fernee on 29/01/2016.
 */
public class FileReader {

    public static String readFile(final String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(FileReader.class.getClassLoader().getResource(path).toURI())), "UTF-8");
        } catch (IOException | URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
