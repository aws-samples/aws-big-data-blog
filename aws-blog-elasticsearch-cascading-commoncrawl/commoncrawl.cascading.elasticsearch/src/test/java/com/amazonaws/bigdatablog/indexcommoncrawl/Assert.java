package com.amazonaws.bigdatablog.indexcommoncrawl;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.fest.assertions.Assertions;

import java.io.File;

/**
 * Shortcut to compare the content of two files.
 */
public class Assert {

    public static void sameContent(String actualOutputPath, String expectedOuputPath) throws Exception {
        String actualContent = contentOf(actualOutputPath);
        String expectedContent = contentOf(expectedOuputPath);
        Assertions.assertThat(actualContent).isEqualTo(expectedContent);
    }

    private static String contentOf(String filePath) throws Exception {
        return Files.toString(new File(filePath), Charsets.UTF_8);
    }

}

