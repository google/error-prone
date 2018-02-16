package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.JAVA_UTIL_FUNCTION_FUNCTION;

import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;

/**
 * Created by ameya on 2/8/18.
 */
public class Mapping {

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


    public static final String NO_MAPPING ="NO MAPPING";
    public static final String TEST = "test";
    public static final String APPLY = "apply";
    public static final String APPLY_AS_LONG = "applyAsLong";
    public static final String APPLY_AS_DOUBLE = "applyAsDouble";
    public static final String APPLY_AS_INT = "applyAsInt";

    public static String getMappedTypeFor(FilteredType ft){
        return getMappedTypeFor(ft.getInterfaceName(),ft.getTypeParameter(0),ft.getTypeParameter(1));
    }

    public static String getMappedTypeFor(String className, String i, String o) {
        if(className.equals(JAVA_UTIL_FUNCTION_FUNCTION)){

            if (i.equals(LANG_INTEGER)) {
                if (o.equals(LANG_INTEGER)) {
                    return INT_UNARY_OPERATOR;
                }
                else if (o.equals(LANG_BOOLEAN)) {
                    return INT_PREDICATE;                     //Function<Integer,Boolean> --> intPredicate
                }
                else if (o.equals(LANG_LONG)) {
                    return INT_TO_LONG_FUNCTION;              //Function<Integer,Long> --> intToLongFunction
                }
                else if (o.equals(LANG_DOUBLE)) {
                    return INT_TO_DOUBLE_FUNCTION;            //Function<Integer,Double> --> intToDoubleFunction
                }
                else  {
                    return INT_FUNCTION + "<" + o + ">";                      //Function<Integer,T> --> intFunction
                }
            }

            //Double Functions
            else if (i.equals(LANG_DOUBLE)) {

                if (o.equals(LANG_DOUBLE)) {
                    return DOUBLE_UNARY_OPERATOR;             //Function<Double,Double> --> doubleUnaryOperator
                }
                else if (o.equals(LANG_BOOLEAN)) {
                    return DOUBLE_PREDICATE;                  //Function<Double,Boolean> --> doublePredicate
                }
                else if (o.equals(LANG_LONG)) {
                    return DOUBLE_TO_LONG_FUNCTION;           //Function<Double,Long> --> doubleToLongFunction
                }
                else if (o.equals(LANG_INTEGER)) {
                    return DOUBLE_TO_INTEGER_FUNCTION;        //Function<Double,Integer> --> doubleToIntFunction
                }
                else {
                    return DOUBLE_FUNCTION + "<" + o + ">";                   //Function<Double,T> --> doubleFunction
                }
            }

            //Long Functions
            else if (i.equals(LANG_LONG)) {
                if (o.equals(LANG_LONG)) {
                    return LONG_UNARY_OPERATOR;               //Function<Long,Long> --> longUnaryOperator
                }

                else if (o.equals(LANG_BOOLEAN)) {
                    return LONG_PREDICATE;                    //Function<Long,Boolean> --> longPredicate
                }
                else if (o.equals(LANG_DOUBLE)) {
                    return LONG_TO_DOUBLE_FUNCTION;           //Function<Long,Double> --> longToDoubleFunction
                }
                else if (o.equals(LANG_INTEGER)) {
                    return LONG_TO_INTEGER_FUNCTION;          //Function<Long,Integer> --> longToIntFunction
                }
                else  {
                    return LONG_FUNCTION+ "<" + o + ">";                     //Function<Long,T> --> longFunction
                }
            }
            else if (o.equals(LANG_BOOLEAN)) {
                return PREDICATE  + "<" + i + ">" ;
            }

            //TO_(DATATYPE)_FUNCTIONS
            else {
                if (o.equals(LANG_INTEGER)) {
                    return TO_INT_FUNCTION + "<" + i + ">";                   //Function<T,Integer> --> toIntFunction
                }
                if (o.equals(LANG_DOUBLE)) {
                    return TO_DOUBLE_FUNCTION+ "<" + i + ">";                //Function<T,Double> --> toDoubleFunction
                }
                if (o.equals(LANG_LONG)) {
                    return TO_LONG_FUNCTION+ "<" + i + ">";                  //Function<T,Long> --> toLongFunction
                }
            }
        }
        return NO_MAPPING;

    }


    public static String getMethodMapping(String name){

//        if(methodName.equals(IS_PRESENT) || methodName.equals(OR_ELSE)){
//            return methodName;
//        }

//        if(name.equals(CHAR_UNARY_OPERATOR))
//            return APPLY;
        if(name.equals(PREDICATE) || name.equals(DOUBLE_PREDICATE)
                || name.equals(INT_PREDICATE) || name.equals(LONG_PREDICATE))
            return TEST;
//        if(methodName.equals(TEST) && (name.equals(BI_PREDICATE) || name.equals(PREDICATE) || name.equals(DOUBLE_PREDICATE)
//                || name.equals(INT_PREDICATE) || name.equals(LONG_PREDICATE)))
//            return TEST;
//        else if(name.equals(BOOLEAN_SUPPLIER))
//            return GET_AS_BOOLEAN;
//        else if(name.equals(DOUBLE_BINARY_OPERATOR))
//            return APPLY_AS_DOUBLE;
//        else if(name.equals(DOUBLE_CONSUMER) || name.equals(INT_CONSUMER) ||  name.equals(LONG_CONSUMER)
//                || name.equals(OBJECT_DOUBLE_CONSUMER) || name.equals(OBJECT_INT_CONSUMER) || name.equals(OBJECT_LONG_CONSUMER))
//            return ACCEPT;|| name.equals(FUNCTION_LIBRARY)
        else if(name.equals(DOUBLE_FUNCTION) || name.equals(INT_FUNCTION) || name.equals(LONG_FUNCTION) )
            return APPLY;
//        else if(name.equals(DOUBLE_SUPPLIER) || name.equals(OPTIONAL_DOUBLE))
//            return GET_AS_DOUBLE;
        else if(name.equals(DOUBLE_TO_LONG_FUNCTION) || name.equals(INT_TO_LONG_FUNCTION)
                || name.equals(LONG_UNARY_OPERATOR) || name.equals(TO_LONG_FUNCTION))
            return APPLY_AS_LONG;
        else if(name.equals(DOUBLE_UNARY_OPERATOR) || name.equals(INT_TO_DOUBLE_FUNCTION) || name.equals(LONG_TO_DOUBLE_FUNCTION)
                || name.equals(TO_DOUBLE_FUNCTION)  )
            return APPLY_AS_DOUBLE;
            //name.equals(INT_BINARY_OPERATOR) |||| name.equals(TO_INT_BI_FUNCTION) || name.equals(TO_DOUBLE_BI_FUNCTION)|| name.equals(DOUBLE_BINARY_OPERATOR)
            //|| name.equals(LONG_BINARY_OPERATOR)  || name.equals(TO_LONG_BI_FUNCTION)
        else if( name.equals(INT_UNARY_OPERATOR) || name.equals(DOUBLE_TO_INTEGER_FUNCTION)
                || name.equals(LONG_TO_INTEGER_FUNCTION)  || name.equals(TO_INT_FUNCTION))
            return APPLY_AS_INT;
//        else if(name.equals(INT_SUPPLIER) || name.equals(OPTIONAL_INT))
//            return GET_AS_INT;
//        else if(name.equals(LONG_SUPPLIER) || name.equals(OPTIONAL_LONG))
//            return GET_AS_LONG;
        else
            return NO_MAPPING;
    }


}

