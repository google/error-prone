package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by ameya on 1/28/18.
 */
public class Constants {
    public static final String LONG = "java.lang.Long";
    public static final String INTEGER = "java.lang.Integer";
    public static final String DOUBLE = "java.lang.Double";
    public static final String BOOLEAN = "java.lang.Boolean";
    public static final Set<String> WRAPPER_CLASSES = ImmutableSet.of(LONG,INTEGER,DOUBLE,BOOLEAN);
    public static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";


    public static final String lambdaExpr = "LAMBDA_EXPRESSION";
    public static final String PARAMETER = "PARAMETER";
    public static final String LOCAL_VARIABLE = "LOCAL_VARIABLE";
    public static final String FIELD = "FIELD";
    public static final String METHOD = "METHOD";


}
