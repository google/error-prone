package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.JAVA_UTIL_FUNCTION_FUNCTION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.WRAPPER_CLASSES;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ameya on 1/17/18.
 */
public class DataFilter {


    // TODO : put in the code for checking all LT.


    public static boolean apply(Tree tree, VisitorState state) {
        return  tree!=null ? apply(ASTHelpers.getType(tree),state) : false;
    }
    /*
    * this method checks if :
    * a. Type is LT
    * b. Type is subtype of LT
    * c. TODO: Type is a container of LT
    * d. TODO: add a way to capture generic types. Function<T,U>
    * */

    public static boolean apply(Type type, VisitorState state) {
        try {
            if (!ASTHelpers.isSubtype(type, state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)) {
                return false;
            }
            return getTypeArgsAsSuper(type, state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)
                    .stream()
                    .anyMatch(x -> WRAPPER_CLASSES.contains(x.toString()));
        }
        catch (Exception e) {
            return false;
        }

    }

    private static List<Type> getTypeArgsAsSuper(Type baseType, Type superType, VisitorState state) {
        Type projectedType = state.getTypes().asSuper(baseType, superType.tsym);
        if (projectedType != null) {
            return projectedType.getTypeArguments();
        }
        return new ArrayList<>();
    }

    public static FilteredType getFilteredType(VariableTree var, VisitorState state){
        FilteredType.Builder ft = FilteredType.newBuilder();
        ft.setInterfaceName(JAVA_UTIL_FUNCTION_FUNCTION); // because i know its always this for now.
        Type t1 = ASTHelpers.getType(var);
        List<String> args = t1.getTypeArguments().stream().map(x -> x.toString()).collect(Collectors.toList());
        if(args.size() == 0)
            args = state.getTypes().interfaces(t1).stream().filter(x ->ASTHelpers.isSameType(x,state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)).findFirst()
                    .map(x -> x.getTypeArguments().stream().map(y -> y.toString()).collect(Collectors.toList())).orElse(new ArrayList<>());
        return  ft.addAllTypeParameter(args).build();
    }


}



