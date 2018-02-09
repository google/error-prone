package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;

import java.util.List;

/**
 * Created by ameya on 1/28/18.
 */
public class QueryProtoBuffData {

    public static List<MethodInvocation> getAllMethdInvc() throws Exception{
//
//        String contents = new String(Files.readAllBytes(Paths.get(pckgName + "METHOD_INVOCATION" + "BinSize.txt")));
//        String[] x = contents.split(" ");
//        List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
//        InputStream is = new FileInputStream(pckgName + "METHOD_INVOCATION" +"Bin.txt");
//        List<MethodInvocation> mthdInvcList = new ArrayList<>();
//        for (Integer c : y) {
//            byte[] b = new byte[c];
//            int i = is.read(b);
//            if (i > 0) {
//                CodedInputStream input = CodedInputStream.newInstance(b);
//                mthdInvcList.add(MethodInvocation.parseFrom(input));
//            }
//        }
//
//        return mthdInvcList;
        return null;
    }


    public static List<MethodInvocation> getAllNewClass() throws Exception{
//
//        String contents = new String(Files.readAllBytes(Paths.get(pckgName + "NEW_CLASS" + "BinSize.txt")));
//        String[] x = contents.split(" ");
//        List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
//        InputStream is = new FileInputStream(pckgName + "NEW_CLASS" +"Bin.txt");
//        List<MethodInvocation> mthdInvcList = new ArrayList<>();
//        for (Integer c : y) {
//            byte[] b = new byte[c];
//            int i = is.read(b);
//            if (i > 0) {
//                CodedInputStream input = CodedInputStream.newInstance(b);
//                mthdInvcList.add(MethodInvocation.parseFrom(input));
//            }
//        }
//
//        return Collections.unmodifiableList(mthdInvcList);
        return null;
    }

    public static List<Assignment> getAllAsgn() throws Exception{

//        String contents = new String(Files.readAllBytes(Paths.get(pckgName + "ASSIGNMENT" + "BinSize.txt")));
//        String[] x = contents.split(" ");
//        List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
//        InputStream is = new FileInputStream(pckgName + "ASSIGNMENT" +"Bin.txt");
//        List<Assignment> asgnList = new ArrayList<>();
//        for (Integer c : y) {
//            byte[] b = new byte[c];
//            int i = is.read(b);
//            if (i > 0) {
//                CodedInputStream input = CodedInputStream.newInstance(b);
//                asgnList.add(Assignment.parseFrom(input));
//            }
//        }
//        return Collections.unmodifiableList(asgnList);
        return null;
    }

    public static List<MethodDeclaration> getAllMethdDecl() {
//        try {
//            String contents = new String(Files.readAllBytes(Paths.get(pckgName + "METHOD" + "BinSize.txt")));
//            String[] x = contents.split(" ");
//            List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
//            InputStream is = new FileInputStream(pckgName +  "METHOD"  + "Bin.txt");
//            List<MethodDeclaration> mthdDclList = new ArrayList<>();
//            for (Integer c : y) {
//                byte[] b = new byte[c];
//                int i = is.read(b);
//                if (i > 0) {
//                    //System.out.println(b);
//                    CodedInputStream input = CodedInputStream.newInstance(b);
//                    mthdDclList.add(MethodDeclaration.parseFrom(input));
//                }
//            }
//            return Collections.unmodifiableList(mthdDclList);
//        }
//        catch(Exception e){
//            System.out.println("Could not get MthdDecl");
//            return new ArrayList<>();
//        }
        return null;

    }

    public static List<Variable> getAllVrbl() throws Exception {

//        String contents = new String(Files.readAllBytes(Paths.get(pckgName +  "VARIABLE" + "BinSize.txt")));
//        String[] x = contents.split(" ");
//        List<Integer> y = Arrays.asList(x).stream().map(String::trim).map(Integer::parseInt).collect(Collectors.toList());
//        InputStream is = new FileInputStream(pckgName +  "VARIABLE" +"Bin.txt");
//        List<Variable> vrblList = new ArrayList<>();
//        for (Integer c : y) {
//            byte[] b = new byte[c];
//            int i = is.read(b);
//            if (i > 0) {
//                CodedInputStream input = CodedInputStream.newInstance(b);
//                vrblList.add(Variable.parseFrom(input));
//            }
//        }
//        return Collections.unmodifiableList(vrblList);
//    }
        return null;
    }
}
