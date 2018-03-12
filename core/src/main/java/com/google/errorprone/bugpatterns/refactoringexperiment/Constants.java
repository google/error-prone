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


    public static final String LAMBDA_EXPRESSION = "LAMBDA_EXPRESSION";
    public static final String PARAMETER = "PARAMETER";
    public static final String METHOD_INVOCATION = "METHOD_INVOCATION";
    public static final String NEW_CLASS = "NEW_CLASS";
    public static final String LOCAL_VARIABLE = "LOCAL_VARIABLE";
    public static final String FIELD = "FIELD";
    public static final String METHOD = "METHOD";
    public static final String CLASS = "CLASS";
    public static final String INTERFACE = "INTERFACE";
    public static final String CONSTRUCTOR= "CONSTRUCTOR";
    public static final String INFERRED_CLASS = "INFERRED_CLASS";
    public static final String REFACTOR_INFO = "RefactorInfo";


    public static final String EDGE_AFFECTED_BY_HIERARCHY ="EDGE_AFFECTED_BY_HIERARCHY";
    public static final String EDGE_PARAM_INDEX = "EDGE_PARAM_INDEX";
    public static final String EDGE_ARG_INDEX = "EDGE_ARG_INDEX";
    public static final String EDGE_PARAM_LAMBDA = "EDGE_PARAM_LAMBDA";
    public static final String EDGE_PASSED_AS_ARG_TO = "EDGE_PASSED_AS_ARG_TO";
    public static final String EDGE_ARG_PASSED = "EDGE_ARG_PASSED";
    public static final String EDGE_ASSIGNED_TO = "EDGE_ASSIGNED_TO";
    public static final String EDGE_ASSIGNED_AS = "EDGE_ASSIGNED_AS";
    public static final String EDGE_RECURSIVE = "EDGE_RECURSIVE";
    public static final String EDGE_METHOD_INVOKED = "EDGE_METHOD_INVOKED";
    public static final String EDGE_REFERENCE = "EDGE_REFERENCE";
    public static final String EDGE_TYPE_INFO = "EDGE_TYPE_INFO";
    public static final String EDGE_OF_TYPE = "EDGE_OF_TYPE";
    public static final String EDGE_PARENT_METHOD = "EDGE_PARENT_METHOD";
    public static final String EDGE_RETURNS = "EDGE_RETURNS";
    public static final String EDGE_OVERRIDES = "EDGE_OVERRIDES";

}
