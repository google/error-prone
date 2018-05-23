package com.google.errorprone.bugpatterns.refactoringexperiment.Mappings;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.BOOLEAN;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.BOOLEAN_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.DOUBLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.DOUBLE_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTEGER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTEGER_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LONG;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LONG_PRIMITIVE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.DOUBLE_CONSUMER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.DOUBLE_CONSUMER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.INT_CONSUMER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.INT_CONSUMER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.LONG_CONSUMER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.LONG_CONSUMER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.ConsumerMapping.mapJavaUtilConsumerToType;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_TO_INT_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_TO_INT_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_TO_LONG_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_TO_LONG_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_UNARY_OPERATOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_UNARY_OPERATOR_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_TO_DOUBLE_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_TO_DOUBLE_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_TO_LONG_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_TO_LONG_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_UNARY_OPERATOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_UNARY_OPERATOR_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_TO_DOUBLE_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_TO_DOUBLE_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_TO_INT_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_TO_INT_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_UNARY_OPERATOR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_UNARY_OPERATOR_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.PREDICATE_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_DOUBLE_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_DOUBLE_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_INT_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_INT_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_LONG_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.TO_LONG_FUNCTION_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.mapJavaUtilFunctionToType;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.PredicateMapping.DOUBLE_PREDICATE_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.PredicateMapping.INT_PREDICATE_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.PredicateMapping.LONG_PREDICATE_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.PredicateMapping.mapJavaUtilPredicateToType;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.BOOLEAN_SUPPLIER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.BOOLEAN_SUPPLIER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.DOUBLE_SUPPLIER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.DOUBLE_SUPPLIER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.INT_SUPPLIER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.INT_SUPPLIER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.LONG_SUPPLIER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.LONG_SUPPLIER_METHOD_MAPPING;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.SupplierMapping.mapJavaUtilSupplierToType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ameya on 5/23/18.
 */
public class Mapping {


    public static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";
    public static final String JAVA_UTIL_FUNCTION_SUPPLIER = "java.util.function.Supplier";
    public static final String JAVA_UTIL_FUNCTION_CONSUMER = "java.util.function.Consumer";
    public static final String JAVA_UTIL_FUNCTION_PREDICATE = "java.util.function.Predicate";
    public static final String NO_MAPPING = "NO MAPPING";
    public static final String TEST = "test";
    public static final String OR = "or";
    public static final String NEGATE = "NEGATE";
    public static final String EQUAL = "isEqual";
    public static final String APPLY = "apply";
    public static final String ACCEPT = "accept";
    public static final String APPLY_AS_LONG = "applyAsLong";
    public static final String APPLY_AS_DOUBLE = "applyAsDouble";
    public static final String APPLY_AS_INT = "applyAsInt";
    public static final String AND_THEN = "andThen";
    public static final String AND = "and";
    public static final String COMPOSE = "compose";
    public static final String IDENTITY = "identity";
    public static final String GET = "get";
    public static final String GET_AS_BOOLEAN = "getAsBoolean";
    public static final String GET_AS_DOUBLE = "getAsDouble";
    public static final String GET_AS_LONG = "getAsLong";
    public static final String GET_AS_INT = "getAsInt";

    public static String getMethodMappingFor(String className,String methodName){
        if(!NO_MAPPING.equals(className)) {
            if (METHOD_MAPPING_FOR.containsKey(className) && METHOD_MAPPING_FOR.get(className).containsKey(methodName)) {
                return METHOD_MAPPING_FOR.get(className).get(methodName);
            }
        }
        return NO_MAPPING;
    }


    public static String preserveType(List<String> typeParameter) {
        StringBuilder preserveType = new StringBuilder("<");
        int counter = 0;
        for (String s : typeParameter) {
            if (counter > 0) {
                preserveType.append("," + s);
            }else{
                preserveType.append(s);
            }
            counter += 1;
        }
        return preserveType.append(">").toString();
    }

    private static ImmutableMap<String, ImmutableMap<String, String>> METHOD_MAPPING_FOR =
            ImmutableMap.<String, ImmutableMap<String, String>>builder()
                    .put(INT_UNARY_OPERATOR, INT_UNARY_OPERATOR_METHOD_MAPPING)
                    .put(DOUBLE_UNARY_OPERATOR, DOUBLE_UNARY_OPERATOR_METHOD_MAPPING)
                    .put(LONG_UNARY_OPERATOR, LONG_UNARY_OPERATOR_METHOD_MAPPING)

                    .put(INT_TO_DOUBLE_FUNCTION, INT_TO_DOUBLE_METHOD_MAPPING)
                    .put(INT_TO_LONG_FUNCTION, INT_TO_LONG_METHOD_MAPPING)
                    // .put(INT_PREDICATE, INT_PREDICATE_MAPPING)
                    .put(INT_FUNCTION, INT_FUNCTION_METHOD_MAPPING)

                    .put(DOUBLE_TO_INT_FUNCTION, DOUBLE_TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(DOUBLE_TO_LONG_FUNCTION, DOUBLE_TO_LONG_METHOD_MAPPING)
                    // .put(DOUBLE_PREDICATE, DOUBLE_PREDICATE_MAPPING)
                    .put(DOUBLE_FUNCTION, DOUBLE_FUNCTION_METHOD_MAPPING)

                    .put(LONG_TO_INT_FUNCTION, LONG_TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(LONG_TO_DOUBLE_FUNCTION, LONG_TO_DOUBLE_METHOD_MAPPING)
                    // .put(LONG_PREDICATE, LONG_PREDICATE_MAPPING)
                    .put(LONG_FUNCTION, LONG_FUNCTION_METHOD_MAPPING)

                    .put(TO_INT_FUNCTION, TO_INT_FUNCTION_METHOD_MAPPING)
                    .put(TO_DOUBLE_FUNCTION, TO_DOUBLE_FUNCTION_METHOD_MAPPING)
                    .put(TO_LONG_FUNCTION, TO_LONG_FUNCTION_METHOD_MAPPING)

                    .put(PREDICATE, PREDICATE_MAPPING)

                    .put(INT_SUPPLIER, INT_SUPPLIER_METHOD_MAPPING)
                    .put(DOUBLE_SUPPLIER,DOUBLE_SUPPLIER_METHOD_MAPPING)
                    .put(LONG_SUPPLIER, LONG_SUPPLIER_METHOD_MAPPING)
                    .put(BOOLEAN_SUPPLIER, BOOLEAN_SUPPLIER_METHOD_MAPPING)

                    .put(INT_CONSUMER, INT_CONSUMER_METHOD_MAPPING)
                    .put(LONG_CONSUMER, LONG_CONSUMER_METHOD_MAPPING)
                    .put(DOUBLE_CONSUMER, DOUBLE_CONSUMER_METHOD_MAPPING)

                    .put(INT_PREDICATE,INT_PREDICATE_METHOD_MAPPING)
                    .put(LONG_PREDICATE,LONG_PREDICATE_METHOD_MAPPING)
                    .put(DOUBLE_PREDICATE,DOUBLE_PREDICATE_METHOD_MAPPING)

                    .build();


    public static final ImmutableMap<String, Function<FilteredType, String>> CLASS_MAPPING_FOR =
            ImmutableMap.<String, Function<FilteredType, String>>builder()
                    .put(JAVA_UTIL_FUNCTION_FUNCTION, mapJavaUtilFunctionToType)//mapJavaUtilSupplierToType
                    .put(JAVA_UTIL_FUNCTION_SUPPLIER, mapJavaUtilSupplierToType)
                    .put(JAVA_UTIL_FUNCTION_CONSUMER, mapJavaUtilConsumerToType)
                    .put(JAVA_UTIL_FUNCTION_PREDICATE,mapJavaUtilPredicateToType).build();

    public static final ImmutableMap<String, String> SPECIALIZE_TO_PRIMITIVE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INTEGER_PRIMITIVE)
                    .put(LONG, LONG_PRIMITIVE)
                    .put(DOUBLE, DOUBLE_PRIMITIVE)
                    .put(BOOLEAN, BOOLEAN_PRIMITIVE)
                    .put(INTEGER_PRIMITIVE, INTEGER_PRIMITIVE)
                    .put(LONG_PRIMITIVE, LONG_PRIMITIVE)
                    .put(DOUBLE_PRIMITIVE, DOUBLE_PRIMITIVE)
                    .put(BOOLEAN_PRIMITIVE, BOOLEAN_PRIMITIVE)
                    .build();

    public static boolean specialiseReturnType(String s) {
        return s.equals(INT_UNARY_OPERATOR) ||
                s.equals(TO_INT_FUNCTION) ||
                s.equals(DOUBLE_TO_INT_FUNCTION) ||
                s.equals(LONG_TO_INT_FUNCTION) ||
                s.equals(DOUBLE_UNARY_OPERATOR) ||
                s.equals(INT_TO_DOUBLE_FUNCTION) ||
                s.equals(LONG_TO_DOUBLE_FUNCTION) ||
                s.equals(TO_DOUBLE_FUNCTION) ||
                s.equals(LONG_UNARY_OPERATOR) ||
                s.equals(INT_TO_LONG_FUNCTION) ||
                s.equals(DOUBLE_TO_LONG_FUNCTION) ||
                s.equals(TO_LONG_FUNCTION) ||

                s.equals(INT_PREDICATE) ||
                s.equals(DOUBLE_PREDICATE) ||
                s.equals(LONG_PREDICATE) ||
                s.equals(PREDICATE) ||
                s.equals(INT_SUPPLIER) ||
                s.equals(LONG_SUPPLIER) ||
                s.equals(DOUBLE_SUPPLIER) ||
                s.equals(BOOLEAN_SUPPLIER);

    }

    public static boolean specialiseInputType(String s) {
        return s.equals(INT_UNARY_OPERATOR) ||
                s.equals(DOUBLE_TO_INT_FUNCTION) ||
                s.equals(LONG_TO_INT_FUNCTION) ||
                s.equals(INT_FUNCTION) ||
                s.equals(LONG_FUNCTION) ||
                s.equals(DOUBLE_FUNCTION) ||

                s.equals(DOUBLE_UNARY_OPERATOR) ||
                s.equals(INT_TO_DOUBLE_FUNCTION) ||
                s.equals(LONG_TO_DOUBLE_FUNCTION) ||
                s.equals(LONG_UNARY_OPERATOR) ||
                s.equals(INT_TO_LONG_FUNCTION) ||
                s.equals(DOUBLE_TO_LONG_FUNCTION) ||

                s.equals(INT_PREDICATE) ||
                s.equals(DOUBLE_PREDICATE) ||
                s.equals(LONG_PREDICATE) ||


                s.equals(INT_CONSUMER) ||
                s.equals(LONG_CONSUMER) ||
                s.equals(DOUBLE_CONSUMER);
    }

    public static ImmutableMap<String,List<Integer>> PRESERVE_ARG = ImmutableMap.<String,List<Integer>>builder()
            .put(DOUBLE_FUNCTION, Arrays.asList(1))
            .put(INT_FUNCTION, Arrays.asList(1))
            .put(LONG_FUNCTION, Arrays.asList(1))
            .put(TO_INT_FUNCTION, Arrays.asList(0))
            .put(TO_LONG_FUNCTION, Arrays.asList(0))
            .put(PREDICATE,Arrays.asList(0))
            .put(TO_DOUBLE_FUNCTION, Arrays.asList(0)).build();

    public static final String changeMethodSelect = "ChangeMethodSelect";
    public static final String castMethodSelect = "CastMethodSelect";
    public static ImmutableSet<String> refactoringInstruction = ImmutableSet.of(changeMethodSelect,castMethodSelect);

    public static final String string_valueOf = "String.valueOf";

    public static final String intValue = "intValue";
    public static final String toString = "toString";
    public static final String cast_int = "(int)";

    public static ImmutableMap<String,String> wrapperMethodMapping = ImmutableMap.<String,String>builder()
            .put(toString, changeMethodSelect + ":" +string_valueOf)
            .put(intValue,castMethodSelect + ":" + cast_int).build();


}
