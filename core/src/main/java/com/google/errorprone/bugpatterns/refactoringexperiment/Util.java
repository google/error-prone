package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.Analysis;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;

import javax.lang.model.element.ElementKind;

/**
 * Created by ameya on 1/17/18.
 */
public class Util {

    private static final String JAVA_UTIL_FUNCTION_FUNCTION = "java.util.function.Function";
    public static final String COLUMN_SEPERATOR = "|";
    public static final String RTRN_TYPE_NOT_FOUND = "RTRN_TYPE_NOT_FOUND";
    private static final String LONG =  "Long";
    private static final String INTEGER =  "Integer";
    private static final String DOUBLE =  "Double";
    private static final String BOOLEAN =  "Boolean";
    private static final String WARNING = "WARNING";
    private static final String CONSTRUCTOR_INIT = "<init>";


    // TODO : put in the code for checking all LT.


    public static boolean isLT(Tree tree,VisitorState state){

        return  isLT(ASTHelpers.getType(tree),state);


    }

    /*
    * this method checks if :
    * a. Type is LT
    * b. Type is subtype of LT
    * c. TODO: Type is a container of LT
    * d. TODO: add a way to capture generic types. Function<T,U>
    * */
    public static boolean isLT(Type t1,VisitorState state){

        return ASTHelpers.isSameType(
                t1, state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION), state)
                ||ASTHelpers.isSubtype(t1,state.getTypeFromString(JAVA_UTIL_FUNCTION_FUNCTION),state)
                &&
                (t1.getTypeArguments().stream().filter(x -> x.toString().contains(INTEGER)|| x.toString().contains(DOUBLE) ||
                        x.toString().contains(LONG) ||  x.toString().contains(BOOLEAN)).count()>0);

    }

    public static String getOwner(Symbol symb) {
        return symb.owner.getKind() +COLUMN_SEPERATOR+symb.owner.toString() + COLUMN_SEPERATOR +
                (symb.owner.getKind().equals(ElementKind.METHOD)|| symb.owner.getKind().equals(ElementKind.CONSTRUCTOR) ?
                        symb.owner.owner.getKind() + COLUMN_SEPERATOR + symb.owner.owner.toString() + COLUMN_SEPERATOR : COLUMN_SEPERATOR);
    }

    public static String getMthdDclName(Symbol.MethodSymbol symb) {
        return symb.isConstructor()? symb.enclClass().toString() : symb.name.toString();
    }
    public static Analysis.anlys.Builder analysisFromTree(Tree tree, Object ... options) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();

        if(options.length>0) a.setIndex((int) options[0]);

        if (tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION)) {
            LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
            a.setKind(lambda.getKind().toString());
            return a;
        }
        else if (tree.getKind().equals(Tree.Kind.MEMBER_REFERENCE)) {
            MemberReferenceTree mr = (MemberReferenceTree) tree;
            a.setKind(mr.getKind().toString());
            return a;
        } else if (tree.getKind().equals(Tree.Kind.METHOD_INVOCATION) || tree.getKind().equals(Tree.Kind.IDENTIFIER)
                || tree.getKind().equals(Tree.Kind.NEW_CLASS) || tree.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
            Symbol symb = ASTHelpers.getSymbol(tree);
            a.setName(getName(symb));
            a.setKind(symb.getKind().toString());
            a.setId(getName(symb) +COLUMN_SEPERATOR +symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString());
            return a;
        }else if(tree.getKind().equals(Tree.Kind.VARIABLE)){
            Symbol.VarSymbol symb = ASTHelpers.getSymbol((VariableTree) tree);
            a.setName(getName(symb));
            a.setKind(symb.getKind().toString());
            a.setId(generateVarId(symb));
            return a;
        }
        else {

            return a.setKind(tree.getKind().toString()).setName(WARNING).setId(WARNING);
        }
    }


    public static String generateMethodId(Symbol.MethodSymbol symb) {

        return Util.getMthdDclName(symb) + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString();
    }

    public static String generateVarId(Symbol.VarSymbol symb) {
        return symb.getQualifiedName().toString() + COLUMN_SEPERATOR + Util.getOwner(symb) + COLUMN_SEPERATOR + symb.type.toString();
    }

    public static Analysis.anlys.Builder analysisFromSymbol(Symbol symb) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();
        a.setName(getName(symb));
        a.setKind(symb.getKind().toString());
        a.setId(getName(symb) +COLUMN_SEPERATOR +symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString());
        return a;

    }


    public static String getName(Symbol symb){

        return symb.name.toString().equals(CONSTRUCTOR_INIT)
                ? symb.owner.name.toString()
                :symb.getQualifiedName().toString();
    }
}
