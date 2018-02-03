package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

/**
 * Created by ameya on 1/17/18.
 */
public class DataFilter {

    private static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";
    private static final String LONG = "Long";
    private static final String INTEGER = "Integer";
    private static final String DOUBLE = "Double";
    private static final String BOOLEAN = "Boolean";


    // TODO : put in the code for checking all LT.


    public static boolean apply(Tree tree, VisitorState state) {

        return apply(ASTHelpers.getType(tree), state);


    }

    /*
    * this method checks if :
    * a. Type is LT
    * b. Type is subtype of LT
    * c. TODO: Type is a container of LT
    * d. TODO: add a way to capture generic types. Function<T,U>
    * */
    public static boolean apply(Type t1, VisitorState state) {

        return ASTHelpers.isSameType(
                t1, state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)
                || ASTHelpers.isSubtype(t1, state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)
                &&
                (t1.getTypeArguments().stream().filter(x -> x.toString().contains(INTEGER) || x.toString().contains(DOUBLE) ||
                        x.toString().contains(LONG) || x.toString().contains(BOOLEAN)).count() > 0);

    }


}
