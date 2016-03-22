package main.java.cz.cvut.ida.nesisl.modules.export.texFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by EL on 12.2.2016.
 */
public class TexFile {

    private final String content;

    public TexFile(String content) {
        this.content = content;
    }

    public File saveAs(String fileName){
        File file = new File(fileName);
        try {
            FileWriter fw = new FileWriter(file);
            fw.write(content);
            fw.close();
        } catch (IOException iox) {
            iox.printStackTrace();
        }
        return file;
    }

    public static int build(File file)  {
        ProcessBuilder builder = new ProcessBuilder("pdflatex.exe", file.getAbsolutePath());
        builder = builder.directory(new File(file.getParent()));
        builder = builder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return process.exitValue();
    }
}
