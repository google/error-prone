package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.JAVA_UTIL_FUNCTION_FUNCTION;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.List;
import java.util.function.Function;

/**
 * Created by ameya on 2/8/18.
 */
public class Mapping {

    public static final String INTEGER = "java.lang.Integer";
    public static final String BOOLEAN = "java.lang.Boolean";
    public static final String LONG = "java.lang.Long";
    public static final String DOUBLE = "java.lang.Double";
    public static final String LONG_PRIMITIVE = "long";
    public static final String INTEGER_PRIMITIVE = "int";
    public static final String DOUBLE_PRIMITIVE = "double";
    public static final String BOOLEAN_PRIMITIVE = "boolean";


    public static final String INT_UNARY_OPERATOR = "java.util.function.IntUnaryOperator";
    public static final String INT_FUNCTION = "java.util.function.IntFunction";
    public static final String INT_PREDICATE = "java.util.function.IntPredicate";
    public static final String INT_TO_LONG_FUNCTION = "java.util.function.IntToLongFunction";
    public static final String INT_TO_DOUBLE_FUNCTION = "java.util.function.IntToDoubleFunction";
    public static final String TO_INT_FUNCTION = "java.util.function.ToIntFunction";
    //Double Functions
    public static final String DOUBLE_UNARY_OPERATOR = "java.util.function.DoubleUnaryOperator";
    public static final String DOUBLE_FUNCTION = "java.util.function.DoubleFunction";
    public static final String TO_DOUBLE_FUNCTION = "java.util.function.ToDoubleFunction";
    public static final String DOUBLE_PREDICATE = "java.util.function.DoublePredicate";
    public static final String DOUBLE_TO_LONG_FUNCTION = "java.util.function.DoubleToLongFunction";
    public static final String DOUBLE_TO_INT_FUNCTION = "java.util.function.DoubleToIntFunction";

    //Long Functions
    public static final String LONG_UNARY_OPERATOR = "java.util.function.LongUnaryOperator";
    public static final String LONG_FUNCTION = "java.util.function.LongFunction";
    public static final String TO_LONG_FUNCTION = "java.util.function.ToLongFunction";
    public static final String LONG_PREDICATE = "java.util.function.LongPredicate";
    public static final String LONG_TO_DOUBLE_FUNCTION = "java.util.function.LongToDoubleFunction";
    public static final String LONG_TO_INT_FUNCTION = "java.util.function.LongToIntFunction";



    public static final String PREDICATE = "java.util.function.Predicate";


    public static final String NO_MAPPING ="NO MAPPING";
    public static final String TEST = "test";
    public static final String APPLY = "apply";
    public static final String APPLY_AS_LONG = "applyAsLong";
    public static final String APPLY_AS_DOUBLE = "applyAsDouble";
    public static final String APPLY_AS_INT = "applyAsInt";
    public static final String AND_THEN ="andThen" ;
    public static final String AND ="and" ;
    public static final String COMPOSE ="compose" ;
    public static final String IDENTITY ="identity" ;

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


    private static final ImmutableMap<String, String> INT_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> DOUBLE_TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> LONG_TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> TO_INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_INT)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();

    private static final ImmutableMap<String, String> LONG_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> DOUBLE_TO_LONG_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> INT_TO_LONG_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> TO_LONG_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_LONG)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();

    private static final ImmutableMap<String, String> DOUBLE_UNARY_OPERATOR_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> INT_TO_DOUBLE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> LONG_TO_DOUBLE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, AND_THEN)
                    .put(COMPOSE, COMPOSE)
                    .put(IDENTITY, IDENTITY).build();
    private static final ImmutableMap<String, String> TO_DOUBLE_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY_AS_DOUBLE)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();

    private static final ImmutableMap<String, String> PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, AND)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    private static final ImmutableMap<String, String> DOUBLE_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    private static final ImmutableMap<String, String> LONG_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    private static final ImmutableMap<String, String> INT_PREDICATE_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();

    private static final ImmutableMap<String, String> DOUBLE_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    private static final ImmutableMap<String, String> INT_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();
    private static final ImmutableMap<String, String> LONG_FUNCTION_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, APPLY)
                    .put(AND_THEN, NO_MAPPING)
                    .put(COMPOSE, NO_MAPPING)
                    .put(IDENTITY, NO_MAPPING).build();

    private static Function<FilteredType, String> mapJavaUtilFunctionToType = ft -> {
        if (JAVA_UTIL_FUNCTION_FUNCTION_IO_SPECIALIZE.containsKey(ft.getTypeParameter(0) + ft.getTypeParameter(1))) {
            return JAVA_UTIL_FUNCTION_FUNCTION_IO_SPECIALIZE.get(ft.getTypeParameter(0) + ft.getTypeParameter(1));
        } else if (JAVA_UTIL_FUNCTION_FUNCTION_I_SPECIALIZE.containsKey(ft.getTypeParameter(0))){
            return JAVA_UTIL_FUNCTION_FUNCTION_I_SPECIALIZE.get(ft.getTypeParameter(0)) + preserveType(ft.getTypeParameter(1));
        }else if (JAVA_UTIL_FUNCTION_FUNCTION_O_SPECIALIZE.containsKey(ft.getTypeParameter(1))) {
            return JAVA_UTIL_FUNCTION_FUNCTION_O_SPECIALIZE.get(ft.getTypeParameter(1)) + preserveType(ft.getTypeParameter(0));
        }
        return NO_MAPPING;
    };

    private static String preserveType(String typeParameter) {
        return "<" + typeParameter + ">";
    }


    public static String getMethodMappingFor(String className,String methodName){
        if(!NO_MAPPING.equals(className)) {
            if (METHOD_MAPPING_FOR.containsKey(className) && METHOD_MAPPING_FOR.get(className).containsKey(methodName)) {
                return METHOD_MAPPING_FOR.get(className).get(methodName);
            }
        }
        return NO_MAPPING;
        }

    private static ImmutableMap<String, ImmutableMap<String, String>> METHOD_MAPPING_FOR =
            ImmutableMap.<String, ImmutableMap<String, String>>builder()
                    .put(INT_UNARY_OPERATOR, INT_UNARY_OPERATOR_METHOD_MAPPING)
                    .put(DOUBLE_UNARY_OPERATOR, DOUBLE_UNARY_OPERATOR_METHOD_MAPPING)
                    .put(LONG_UNARY_OPERATOR, LONG_UNARY_OPERATOR_METHOD_MAPPING)

                    .put(INT_TO_DOUBLE_FUNCTION, INT_TO_DOUBLE_METHOD_MAPPING)
                    .put(INT_TO_LONG_FUNCTION, INT_TO_LONG_METHOD_MAPPING)
                    .put(INT_PREDICATE, INT_PREDICATE_MAPPING)
                    .put(INT_FUNCTION, INT_FUNCTION_METHOD_MAPPING)

                    .put(DOUBLE_TO_INT_FUNCTION, DOUBLE_TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(DOUBLE_TO_LONG_FUNCTION, DOUBLE_TO_LONG_METHOD_MAPPING)
                    .put(DOUBLE_PREDICATE, DOUBLE_PREDICATE_MAPPING)
                    .put(DOUBLE_FUNCTION, DOUBLE_FUNCTION_METHOD_MAPPING)

                    .put(LONG_TO_INT_FUNCTION, LONG_TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(LONG_TO_DOUBLE_FUNCTION, LONG_TO_DOUBLE_METHOD_MAPPING)
                    .put(LONG_PREDICATE, LONG_PREDICATE_MAPPING)
                    .put(LONG_FUNCTION,LONG_FUNCTION_METHOD_MAPPING)

                    .put(TO_INT_FUNCTION, TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(TO_DOUBLE_FUNCTION, TO_DOUBLE_FUNCTION_METHOD_MAPPING)
                    .put(TO_LONG_FUNCTION, TO_LONG_FUNCTION_METHOD_MAPPING)

                    .put(PREDICATE, PREDICATE_MAPPING)


                    .build();


    public static final ImmutableMap<String, Function<FilteredType, String>> CLASS_MAPPING_FOR =
            ImmutableMap.<String, Function<FilteredType, String>>builder()
                    .put(JAVA_UTIL_FUNCTION_FUNCTION, mapJavaUtilFunctionToType).build();

    public static final ImmutableMap<String, String> SPECIALIZE_TO_PRIMITIVE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INTEGER_PRIMITIVE)
                    .put(LONG, LONG_PRIMITIVE)
                    .put(DOUBLE, DOUBLE_PRIMITIVE)
                    .put(BOOLEAN, BOOLEAN_PRIMITIVE)
                    .build();

    /**
     * This map, takes specialised functional interfaces as key,
     * and stores the indicses of the type argument to be preserved after migration
     * as the value.
     * Eg.
     * When we attempt to migrate a type from Function<Foo,Integer> to IntFunction<Foo>,
     * we need to preserve the first type argument of Function<Foo,Integer>
     * Similarly Function<Integer,Bar> -> IntFunction<Bar>, where we preserve the second
     * type arguement of the type.
     */
    public static ImmutableMap<String,List<Integer>> PRESERVE_ARG = ImmutableMap.<String,List<Integer>>builder()
            .put(DOUBLE_FUNCTION, ImmutableList.of(1))
            .put(INT_FUNCTION, ImmutableList.of(1))
            .put(LONG_FUNCTION, ImmutableList.of(1))
            .put(TO_INT_FUNCTION, ImmutableList.of(0))
            .put(TO_LONG_FUNCTION, ImmutableList.of(0))
            .put(PREDICATE,ImmutableList.of(0))
            .put(TO_DOUBLE_FUNCTION, ImmutableList.of(0)).build();
}

