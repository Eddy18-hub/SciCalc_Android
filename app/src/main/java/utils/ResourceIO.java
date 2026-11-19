package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Resource IO to automate read/write on configuration/resources.
 * Android port: uses java.io.File instead of java.nio.file (not available before API 26).
 *
 * @author Good Sign
 */
public class ResourceIO {

    private final File file;
    private final Charset charset = Charset.forName("US-ASCII");

    public ResourceIO(final File file) {
        this.file = file;
    }

    public ResourceIO(final String path) {
        this.file = new File(path);
    }

    public boolean exists() {
        return file.exists();
    }

    public boolean readLines(final Consumer<String> lineConsumer) {
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), charset))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineConsumer.accept(line);
                }
                
                return true;
            } catch (IOException x) {
                System.err.format("IOException: %s%n", x);
                return false;
            }
        }

        return false;
    }

    /** Overwrites the file with the lines produced by [lineSupplier] (until it returns null). */
    public boolean writeLines(final Supplier<String> lineSupplier) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), charset))) {
            String line;
            while ((line = lineSupplier.get()) != null) {
                writer.write(line, 0, line.length());
                writer.newLine();
            }
            
            return true;
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
            return false;
        }
    }
    
    public String getFileame() {
        return file.toString();
    }
}
