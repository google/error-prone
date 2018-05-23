package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.SOURCE_TYPES;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.SOURCE_TYPE_PARAMETERS;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.WRAPPER_CLASSES;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.FilteredTypeOuterClass.FilteredType;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
            Optional<String> filteredType = SOURCE_TYPES.stream().filter(x -> ASTHelpers.isSubtype(type, state.getTypeFromString(x), state)).findFirst();
            if (!filteredType.isPresent()) {
                return false;
            }
            return getTypeArgsAsSuper(type, state.getTypeFromString(filteredType.get()), state)
                    .stream()
                    .anyMatch(x -> SOURCE_TYPE_PARAMETERS.contains(x.toString()));
        } catch (Exception e) {
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


    public static Optional<FilteredType> getFilteredType(Tree tree, VisitorState state) {
        if (apply(tree, state)) {
            String filteredType = SOURCE_TYPES.stream()
                    .filter(x -> ASTHelpers.isSubtype(ASTHelpers.getType(tree), state.getTypeFromString(x), state)).findFirst().get();
            FilteredType.Builder filteredTypeMsg = FilteredType.newBuilder();
            filteredTypeMsg.setInterfaceName(filteredType);
            getTypeArgsAsSuper(ASTHelpers.getType(tree), state.getTypeFromString(filteredType), state)
                    .stream()
                    .forEach(t -> filteredTypeMsg.addTypeParameter(t.toString()));
            return Optional.of(filteredTypeMsg.build());
        }

        return Optional.empty();
    }

    public static boolean isOfTypePrimitiveWrapper(Tree tree) {
        try {
            Type t = ASTHelpers.getSymbol(tree).type;
            if (t == null)
                return false;
            return WRAPPER_CLASSES.contains(t.toString());
        }catch (Exception e){
            return  false;
        }
    }


}



