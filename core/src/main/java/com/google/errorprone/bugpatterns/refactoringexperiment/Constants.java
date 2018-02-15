package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Created by ameya on 1/28/18.
 */
public class Constants {


    public static final String REFACTOR_INFO = "RefactorInfo";
    public static final String RTRN_TYPE_NOT_FOUND = "RTRN_TYPE_NOT_FOUND";

    public static final String LONG = "java.lang.Long";
    public static final String INTEGER = "java.lang.Integer";
    public static final String DOUBLE = "java.lang.Double";
    public static final String BOOLEAN = "java.lang.Boolean";
    public static final Set<String> WRAPPER_CLASSES = ImmutableSet.of(LONG,INTEGER,DOUBLE,BOOLEAN);

    public static final String lambdaExpr = "LAMBDA_EXPRESSION";
    public static final String PARAMETER = "PARAMETER";
    public static final String LOCAL_VARIABLE = "LOCAL_VARIABLE";
    public static final String FIELD = "FIELD";
    public static final String METHOD = "METHOD";

    public static final String LANG_INTEGER = "java.lang.Integer";
    public static final String LANG_BOOLEAN = "java.lang.Boolean";
    public static final String LANG_LONG = "java.lang.Long";
    public static final String LANG_DOUBLE = "java.lang.Double";
    public static final String INT_UNARY_OPERATOR = "java.util.function.IntUnaryOperator";
    public static final String INT_FUNCTION = "java.util.function.IntFunction";
    public static final String TO_INT_FUNCTION = "java.util.function.ToIntFunction";
    public static final String INT_PREDICATE = "java.util.function.IntPredicate";

    public static final String INT_TO_LONG_FUNCTION = "java.util.function.IntToLongFunction";
    public static final String INT_TO_DOUBLE_FUNCTION = "java.util.function.IntToDoubleFunction";

    //Double Functions
    public static final String DOUBLE_UNARY_OPERATOR = "java.util.function.DoubleUnaryOperator";
    public static final String DOUBLE_FUNCTION = "java.util.function.DoubleFunction";
    public static final String TO_DOUBLE_FUNCTION = "java.util.function.ToDoubleFunction";
    public static final String DOUBLE_PREDICATE = "java.util.function.DoublePredicate";
    public static final String DOUBLE_TO_LONG_FUNCTION = "java.util.function.DoubleToLongFunction";
    public static final String DOUBLE_TO_INTEGER_FUNCTION = "java.util.function.DoubleToIntFunction";

    //Long Functions
    public static final String LONG_UNARY_OPERATOR = "java.util.function.LongUnaryOperator";
    public static final String LONG_FUNCTION = "java.util.function.LongFunction";
    public static final String TO_LONG_FUNCTION = "java.util.function.TolongFunction";
    public static final String LONG_PREDICATE = "java.util.function.LongPredicate";
    public static final String LONG_TO_DOUBLE_FUNCTION = "java.util.function.LongToDoubleFunction";
    public static final String LONG_TO_INTEGER_FUNCTION = "java.util.function.LongToIntFunction";
    public static final String PREDICATE = "java.util.function.Predicate";
    public static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";

    public static final String NO_MAPPING ="NO MAPPING";
    public static final String TEST = "test";
    public static final String APPLY = "apply";
    public static final String APPLY_AS_LONG = "applyAsLong";
    public static final String APPLY_AS_DOUBLE = "applyAsDouble";
    public static final String APPLY_AS_INT = "applyAsInt";
}
