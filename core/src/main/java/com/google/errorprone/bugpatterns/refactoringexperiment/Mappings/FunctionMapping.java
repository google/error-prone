package com.google.errorprone.bugpatterns.refactoringexperiment.Mappings;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.BOOLEAN;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.DOUBLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTEGER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LONG;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.Mapping.*;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.Mapping.NO_MAPPING;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.Arrays;
import java.util.function.Function;

/**
 * Created by ameya on 5/23/18.
 */
public class FunctionMapping {
    public static final String INT_UNARY_OPERATOR = "java.util.function.IntUnaryOperator";
    public static final String INT_FUNCTION = "java.util.function.IntFunction";
    public static final String TO_INT_FUNCTION = "java.util.function.ToIntFunction";
    public static final String INT_PREDICATE = "java.util.function.IntPredicate";

    public static final String INT_TO_LONG_FUNCTION = "java.util.function.IntToLongFunction";
    public static final String INT_TO_DOUBLE_FUNCTION = "java.util.function.IntToDoubleFunction";

    //Double Functions
    public static final String DOUBLE_UNARY_OPERATOR = "java.util.function.DoubleUnaryOperator";
    public static final String DOUBLE_PREDICATE = "java.util.function.DoublePredicate";
    public static final String DOUBLE_TO_LONG_FUNCTION = "java.util.function.DoubleToLongFunction";
    public static final String DOUBLE_TO_INT_FUNCTION = "java.util.function.DoubleToIntFunction";

    //Long Functions
    public static final String LONG_UNARY_OPERATOR = "java.util.function.LongUnaryOperator";
    public static final String LONG_FUNCTION = "java.util.function.LongFunction";
    public static final String TO_LONG_FUNCTION = "java.util.function.ToLongFunction";
    public static final String TO_DOUBLE_FUNCTION = "java.util.function.ToDoubleFunction";
    public static final String DOUBLE_FUNCTION = "java.util.function.DoubleFunction";
    public static final String LONG_PREDICATE = "java.util.function.LongPredicate";
    public static final String LONG_TO_DOUBLE_FUNCTION = "java.util.function.LongToDoubleFunction";
    public static final String LONG_TO_INT_FUNCTION = "java.util.function.LongToIntFunction";
    public static final String PREDICATE = "java.util.function.Predicate";


    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_FUNCTION_IO_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER + INTEGER, INT_UNARY_OPERATOR)
                    .put(INTEGER + BOOLEAN, INT_PREDICATE)
                    .put(INTEGER + DOUBLE, INT_TO_DOUBLE_FUNCTION)
                    .put(INTEGER + LONG, INT_TO_LONG_FUNCTION)

                    .put(LONG + LONG, LONG_UNARY_OPERATOR)
                    .put(LONG + INTEGER, LONG_TO_INT_FUNCTION)
                    .put(LONG + DOUBLE, LONG_TO_DOUBLE_FUNCTION)
                    .put(LONG + BOOLEAN, LONG_PREDICATE)

                    .put(DOUBLE + DOUBLE, DOUBLE_UNARY_OPERATOR)
                    .put(DOUBLE + INTEGER, DOUBLE_TO_INT_FUNCTION)
                    .put(DOUBLE + LONG, DOUBLE_TO_LONG_FUNCTION)
                    .put(DOUBLE + BOOLEAN, DOUBLE_PREDICATE)
                    .build();
    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_FUNCTION_I_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INT_FUNCTION)
                    .put(LONG + BOOLEAN, LONG_FUNCTION)
                    .put(DOUBLE + DOUBLE, DOUBLE_FUNCTION)
                    .build();
    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_FUNCTION_O_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, TO_INT_FUNCTION)
                    .put(LONG, TO_LONG_FUNCTION)
                    .put(DOUBLE, TO_DOUBLE_FUNCTION)
                    .put(BOOLEAN, PREDICATE)
                    .build();
    static Function<FilteredType, String> mapJavaUtilFunctionToType = ft -> {
        if (JAVA_UTIL_FUNCTION_FUNCTION_IO_SPECIALIZE.containsKey(ft.getTypeParameter(0) + ft.getTypeParameter(1))) {
            return JAVA_UTIL_FUNCTION_FUNCTION_IO_SPECIALIZE.get(ft.getTypeParameter(0) + ft.getTypeParameter(1));
        } else if (JAVA_UTIL_FUNCTION_FUNCTION_I_SPECIALIZE.containsKey(ft.getTypeParameter(0))) {
            return JAVA_UTIL_FUNCTION_FUNCTION_I_SPECIALIZE.get(ft.getTypeParameter(0)) + Mapping.preserveType(Arrays.asList(ft.getTypeParameter(1)));
        } else if (JAVA_UTIL_FUNCTION_FUNCTION_O_SPECIALIZE.containsKey(ft.getTypeParameter(1))) {
            return JAVA_UTIL_FUNCTION_FUNCTION_O_SPECIALIZE.get(ft.getTypeParameter(1)) + Mapping.preserveType(Arrays.asList(ft.getTypeParameter(0)));
        }
        return NO_MAPPING;
    };
    static final ImmutableMap<String, String> INT_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> DOUBLE_TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> LONG_TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> LONG_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> DOUBLE_TO_LONG_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> INT_TO_LONG_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> TO_LONG_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> DOUBLE_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> INT_TO_DOUBLE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> LONG_TO_DOUBLE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    static final ImmutableMap<String, String> TO_DOUBLE_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, AND)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> DOUBLE_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> LONG_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> INT_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> DOUBLE_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    static final ImmutableMap<String, String> LONG_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
}
