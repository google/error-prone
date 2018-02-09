package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;


import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.*;

/**
 * Created by ameya on 2/8/18.
 */
public class Mapping {

    public static String getMappedTypeFor(String className, String i, String o) {
        if(className.equals(JAVA_UTIL_FUNCTION_FUNCTION)){

        if (i.equals(LANG_INTEGER)) {
            if (o.equals(LANG_INTEGER)) {
                return INT_UNARY_OPERATOR;
            }
            if (o.equals(LANG_BOOLEAN)) {
                return INT_PREDICATE;                     //Function<Integer,Boolean> --> intPredicate
            }
            if (o.equals(LANG_LONG)) {
                return INT_TO_LONG_FUNCTION;              //Function<Integer,Long> --> intToLongFunction
            }
            if (o.equals(LANG_DOUBLE)) {
                return INT_TO_DOUBLE_FUNCTION;            //Function<Integer,Double> --> intToDoubleFunction
            }
            if (!o.equals(LANG_INTEGER)) {
                return INT_FUNCTION + "<" + o + ">";                      //Function<Integer,T> --> intFunction
            }
        }

        //Double Functions
        else if (i.equals(LANG_DOUBLE)) {

            if (o.equals(LANG_DOUBLE)) {
                return DOUBLE_UNARY_OPERATOR;             //Function<Double,Double> --> doubleUnaryOperator
            }
            if (!o.equals(LANG_DOUBLE)) {
                return DOUBLE_FUNCTION + "<" + o + ">";                   //Function<Double,T> --> doubleFunction
            }
            if (o.equals(LANG_BOOLEAN)) {
                return DOUBLE_PREDICATE;                  //Function<Double,Boolean> --> doublePredicate
            }
            if (o.equals(LANG_LONG)) {
                return DOUBLE_TO_LONG_FUNCTION;           //Function<Double,Long> --> doubleToLongFunction
            }
            if (o.equals(LANG_INTEGER)) {
                return DOUBLE_TO_INTEGER_FUNCTION;        //Function<Double,Integer> --> doubleToIntFunction
            }
        }

        //Long Functions
        else if (i.equals(LANG_LONG)) {
            if (o.equals(LANG_LONG)) {
                return LONG_UNARY_OPERATOR;               //Function<Long,Long> --> longUnaryOperator
            }
            if (!o.equals(LANG_LONG)) {
                return LONG_FUNCTION+ "<" + o + ">";                     //Function<Long,T> --> longFunction
            }
            if (o.equals(LANG_BOOLEAN)) {
                return LONG_PREDICATE;                    //Function<Long,Boolean> --> longPredicate
            }
            if (o.equals(LANG_DOUBLE)) {
                return LONG_TO_DOUBLE_FUNCTION;           //Function<Long,Double> --> longToDoubleFunction
            }
            if (o.equals(LANG_INTEGER)) {
                return LONG_TO_INTEGER_FUNCTION;          //Function<Long,Integer> --> longToIntFunction
            }
        }
        else if (o.equals(LANG_BOOLEAN)) {
            return PREDICATE;
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


}

