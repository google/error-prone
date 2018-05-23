package com.google.errorprone.bugpatterns.refactoringexperiment.Mappings;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.DOUBLE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.INTEGER;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.LONG;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.DOUBLE_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.INT_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.FunctionMapping.LONG_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.Mapping.*;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.function.Function;

/**
 * Created by ameya on 5/23/18.
 */
public class PredicateMapping {




    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INT_PREDICATE)
                    .put(LONG , LONG_PREDICATE)
                    .put(DOUBLE , DOUBLE_PREDICATE)
                    .build();

    static Function<FilteredType, String> mapJavaUtilPredicateToType = ft -> {
        if (JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE.containsKey(ft.getTypeParameter(0))) {
            return JAVA_UTIL_FUNCTION_BIFUNCTION_I_SPECIALIZE.get(ft.getTypeParameter(0));
        }
        return NO_MAPPING;
    };
    static final ImmutableMap<String, String> INT_PREDICATE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(APPLY, TEST)
                    .put(TEST, TEST)
                    .put(OR, OR)
                    .put(NEGATE,NEGATE)
                    .put(EQUAL,NO_MAPPING)
                    .put(AND,AND).build();

    static final ImmutableMap<String, String> DOUBLE_PREDICATE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(TEST, TEST)
                    .put(OR, OR)
                    .put(NEGATE,NEGATE)
                    .put(EQUAL,NO_MAPPING)
                    .put(AND,AND).build();

    static final ImmutableMap<String, String> LONG_PREDICATE_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(TEST, TEST)
                    .put(OR, OR)
                    .put(NEGATE,NEGATE)
                    .put(EQUAL,NO_MAPPING)
                    .put(AND,AND).build();
}
