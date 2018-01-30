package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.protobuf.GeneratedMessageV3;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by ameya on 1/24/18.
 */
public class ProtoBuffPersist {

    public static final String pckgName = "/Users/ameya/pilot_plugin/ProtoBuffOutput/";

    public static void write(GeneratedMessageV3.Builder builder,String name) {

        String nameBin = pckgName+name+"Bin.txt";
        String nameBinSize = pckgName+name+"BinSize.txt";
        String name1 = pckgName+name+".txt";
        try {
//            String t = TextFormat.printToString(builder);
//            FileOutputStream output1 = new FileOutputStream(name1,true);
//            output1.write(t.getBytes(Charset.forName("UTF-8")));

            FileOutputStream outputSize = new FileOutputStream(nameBinSize,true);
            String size = builder.build().getSerializedSize() + " ";
            outputSize.write(size.getBytes(Charset.forName("UTF-8")));
            FileOutputStream output = new FileOutputStream(nameBin,true);
            builder.build().writeTo(output);
            output.close();
        } catch (FileNotFoundException e) {
            System.out.println("File not found.  Creating a new file.");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
