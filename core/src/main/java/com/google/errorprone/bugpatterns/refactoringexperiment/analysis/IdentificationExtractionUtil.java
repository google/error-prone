package com.google.errorprone.bugpatterns.refactoringexperiment.analysis;

import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification.Builder;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;

import java.util.Optional;

import javax.lang.model.element.ElementKind;

/**
 * Created by ameya on 3/12/18.
 */
public class IdentificationExtractionUtil {

    /**
     * This method returns Identification for a given tree.
     * @param tree : AST for which Identification has to be extracted
     * @return Identification for the tree.
     * This method is used to get Identification for any ASTElement.
     */
    public static Identification infoOfTree(Tree tree) {
        Builder infor = infoFromTree(tree).orElse(null);
        if (infor == null)
            if (tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION))
                infor = checksInsideLambda(tree);
            else
                infor = Identification.newBuilder().setKind(tree.getKind().toString());

        return infor.build();
    }

    /**
     * This method returns Identification for a given tree.
     * @param tree AST for which Identification has to be extracted
     * @return Identification for the tree.
     * This method attempts to extract type information from the tree,using by trying to resolve
     * its symbol. If this method fails to do so, it returns a empty optional.
     *
     */

    public static Optional<Identification.Builder> infoFromTree(Tree tree) {
        try {
            Symbol symb = ASTHelpers.getSymbol(tree);
            Identification.Builder id = Identification.newBuilder();
            id.setName(getName(symb))
                    .setKind(getKindFromTree(tree).orElse(symb.getKind().toString()))
                    .setOwner(getASTOwner(symb))
                    .setType(symb.type.toString());
            return java.util.Optional.of(id);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * This method returns Identification for a given symbol.
     * @param symb symbol for which Identification has to be extracted
     * @return Identification for the tree.
     */

    public static Optional<Identification.Builder> infoFromSymbol(Symbol symb) {
        try {
            Identification.Builder id = Identification.newBuilder();
            id.setName(getName(symb))
                    .setKind((symb.getKind().toString()))
                    .setOwner(getASTOwner(symb))
                    .setType(symb.type.toString());
            return java.util.Optional.of(id);
        } catch (Exception e) {
            return java.util.Optional.empty();
        }
    }

    private static Identification.Builder checksInsideLambda(Tree tree) {
        LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
        Identification.Builder id = Identification.newBuilder();
        id.setKind(tree.getKind().toString())
                .setType(ASTHelpers.getType(lambda).toString());
        return id;
    }

    private static Identification getASTOwner(Symbol symb) {

        if (symb.owner.getKind().equals(ElementKind.PACKAGE)) {
            PackageSymbol pkgSymb = (PackageSymbol) symb.owner;
            return Identification.newBuilder().setName(pkgSymb.fullname.toString())
                    .setKind(ElementKind.PACKAGE.toString()).build();
        }

        return infoFromSymbol(symb.owner).get()
                .build();
    }

    private static java.util.Optional<String> getKindFromTree(Tree tree) {
        if (tree.getKind().equals(Kind.METHOD_INVOCATION) || tree.getKind().equals(Kind.NEW_CLASS))
            return java.util.Optional.of(tree.getKind().toString());
        return java.util.Optional.empty();
    }


    private static String getName(Symbol symb) {
        if (symb.name != null)
            return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
        else
            return "";
    }

}
