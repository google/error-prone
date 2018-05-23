package com.google.errorprone.bugpatterns.refactoringexperiment.Mappings;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.DOUBLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTEGER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LONG;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.Mapping.*;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.function.Function;

/**
 * Created by ameya on 5/23/18.
 */
public class ConsumerMapping {


    public static final String INT_CONSUMER = "java.util.function.IntConsumer";
    public static final String DOUBLE_CONSUMER = "java.util.function.DoubleConsumer";
    public static final String LONG_CONSUMER = "java.util.function.LongConsumer";

    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INT_CONSUMER)
                    .put(LONG , DOUBLE_CONSUMER)
                    .put(DOUBLE , LONG_CONSUMER)
                    .build();

    static Function<FilteredType, String> mapJavaUtilConsumerToType = ft -> {
        if (JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE.containsKey(ft.getTypeParameter(0))) {
            return JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE.get(ft.getTypeParameter(0));
        }
        return NO_MAPPING;
    };
    static final ImmutableMap<String, String> INT_CONSUMER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(ACCEPT, ACCEPT)
                    .put(AND_THEN,AND_THEN).build();
    static final ImmutableMap<String, String> DOUBLE_CONSUMER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(ACCEPT, ACCEPT)
                    .put(AND_THEN,AND_THEN).build();
    static final ImmutableMap<String, String> LONG_CONSUMER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(ACCEPT, ACCEPT)
                    .put(AND_THEN,AND_THEN).build();
}
