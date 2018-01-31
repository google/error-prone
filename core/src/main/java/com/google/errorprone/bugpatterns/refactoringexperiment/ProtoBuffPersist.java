package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.protobuf.GeneratedMessageV3;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by ameya on 1/24/18.
 *
 * @ALI: This implementation will be changed, right?
 */
public class ProtoBuffPersist {

    public static final String pckgName = "/Users/ameya/pilot_plugin/ProtoBuffOutput/";
    public static final String fileNameSuffix = "Bin.txt";

    public static void write(GeneratedMessageV3.Builder builder, String name) {
        String nameBin = pckgName + name + fileNameSuffix;
        try {
            FileOutputStream output = new FileOutputStream(nameBin, true);
            builder.build().writeTo(output);
            output.close();
        } catch (FileNotFoundException e) {
            //logger.error("File not found");
        } catch (IOException e) {
            //logger.error("File could not be read");
        }
    }
}
