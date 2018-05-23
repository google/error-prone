package com.google.errorprone.bugpatterns.refactoringexperiment.Mappings;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.*;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Mappings.Mapping.*;

import com.google.common.collect.ImmutableMap;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

import java.util.function.Function;

/**
 * Created by ameya on 5/23/18.
 */
public class SupplierMapping {


    public static final String INT_SUPPLIER = "java.util.function.IntSupplier";
    public static final String DOUBLE_SUPPLIER = "java.util.function.DoubleSupplier";
    public static final String LONG_SUPPLIER = "java.util.function.LongSupplier";
    public static final String BOOLEAN_SUPPLIER = "java.util.function.BooleanSupplier";

    private static final ImmutableMap<String, String> JAVA_UTIL_FUNCTION_BIFUNCTION_O_SPECIALIZE =
            ImmutableMap.<String, String>builder()
                    .put(INTEGER, INT_SUPPLIER)
                    .put(LONG , DOUBLE_SUPPLIER)
                    .put(DOUBLE , LONG_SUPPLIER)
                    .put(BOOLEAN, BOOLEAN_SUPPLIER)
                    .build();

    static Function<FilteredType, String> mapJavaUtilSupplierToType = ft -> {
        if (JAVA_UTIL_FUNCTION_BIFUNCTION_O_SPECIALIZE.containsKey(ft.getTypeParameter(0))) {
            return JAVA_UTIL_FUNCTION_BIFUNCTION_O_SPECIALIZE.get(ft.getTypeParameter(0));
        }
        return NO_MAPPING;
    };
    static final ImmutableMap<String, String> INT_SUPPLIER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(GET, GET_AS_INT).build();
    static final ImmutableMap<String, String> DOUBLE_SUPPLIER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(GET, GET_AS_DOUBLE).build();
    static final ImmutableMap<String, String> LONG_SUPPLIER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(GET, GET_AS_LONG).build();
    static final ImmutableMap<String, String> BOOLEAN_SUPPLIER_METHOD_MAPPING =
            ImmutableMap.<String, String>builder()
                    .put(GET, GET_AS_BOOLEAN).build();
}
