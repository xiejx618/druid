package org.exam.util;

import java.io.*;
import java.nio.charset.Charset;

public abstract class FileUtils {

    public static final int BUFFER_SIZE = 4096;

    private FileUtils() {
        throw new UnsupportedOperationException();
    }

    public static String copyToString(InputStream in, Charset charset) {
        if (in == null) {
            return "";
        }
        InputStream stream = null;
        InputStreamReader reader = null;
        try {
            StringBuilder out = new StringBuilder();
            stream = new BufferedInputStream(in);
            reader = new InputStreamReader(stream, charset);
            char[] buffer = new char[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                out.append(buffer, 0, bytesRead);
            }
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static String copyToString(File in, Charset charset) {
        if (in == null) {
            return "";
        }
        FileInputStream stream = null;
        InputStreamReader reader = null;
        try {
            stream = new FileInputStream(in);
            reader = new InputStreamReader(stream, charset);
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[4096];
            int bytesRead;
            while ((bytesRead = reader.read(buffer)) != -1) {
                out.append(buffer, 0, bytesRead);
            }
            return out.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
    }


    public static void copyToFile(byte[] in, File file) {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            outputStream.write(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

}
