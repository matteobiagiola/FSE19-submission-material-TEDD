package org.mb.tedd.utils;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;


public class FileUtils {

    private final static Logger logger = Logger.getLogger(FileUtils.class.getName());

    /**
     * Write string to file
     *
     * @param fileName
     *            - name of the file to write to
     * @param content
     *            - text to write into the file
     */
    public static void writeFile(String content, String fileName) {
        try {
            Writer writer = new PrintWriter(new File(fileName));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            logger.error("Error while writing file " + fileName + " , " + e.getMessage());
        }
    }
}
