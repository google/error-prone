package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.Analysis;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDecl;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.Variable;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;

@AutoService(BugChecker.class)
@BugPattern(
        name = "TypeUsageCollector",
        category = JDK,
        summary = "String formatting inside print method",
        severity = ERROR,
        linkType = CUSTOM,
        link = "example.com/bugpattern/MyCustomCheck"
)
public class TypeUsageCollector extends BugChecker implements BugChecker.MethodTreeMatcher, BugChecker.MethodInvocationTreeMatcher, BugChecker.NewClassTreeMatcher, BugChecker.VariableTreeMatcher
        , BugChecker.AssignmentTreeMatcher, BugChecker.ClassTreeMatcher {

    public static final String COLUMN_SEPERATOR = "|";
    public static final String RTRN_TYPE_NOT_FOUND = "RTRN_TYPE_NOT_FOUND";
    private static final String WARNING = "WARNING";
    private static final String CONSTRUCTOR_INIT = "<init>";

    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(methodTree);
        List<? extends VariableTree> params = methodTree.getParameters();
        boolean paramsMatter = params.stream().filter(x -> LegacyTypeInfoProvider.isLT(x, state)).collect(Collectors.toList()).size() > 0;
        boolean returnMatter = LegacyTypeInfoProvider.isLT(methodTree.getReturnType(), state);
        if (paramsMatter || returnMatter) {
            MethodDeclaration.MthdDcl.Builder mthdDcl = MethodDeclaration.MthdDcl.newBuilder();
            mthdDcl.setName(getMthdDclName(symb))
                    .setOwner(symb.owner.toString())
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(generateMethodId(symb));

            mthdDcl.setReturnType(ASTHelpers.getType(methodTree.getReturnType()) != null ? ASTHelpers.getType(methodTree.getReturnType()).toString() : RTRN_TYPE_NOT_FOUND);

            List<String> y = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().map(x -> x.owner.toString()).collect(Collectors.toList());

            if (y != null && !y.isEmpty()) {
                mthdDcl.setSuperMethodIn(y.get(0));
            }
            params.stream().filter(x -> LegacyTypeInfoProvider.isLT(x, state)).map(x -> analysisFromTree(x, params.indexOf(x))).forEach(mthdDcl::addParam);
            ProtoBuffPersist.write(mthdDcl, methodTree.getKind().toString());
        }
        return null;
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(tree);
        List<Symbol.VarSymbol> params = symb.getParameters();

        boolean retLt = LegacyTypeInfoProvider.isLT(tree, state);
        boolean paramLT = params.stream().filter(x -> LegacyTypeInfoProvider.isLT(x.type, state)).count() > 0;
        boolean ofLT = LegacyTypeInfoProvider.isLT(symb.owner.type, state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;

        if (retLt || paramLT || ofLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(generateMethodId(symb));

            if (paramLT) {
                params.stream().filter(x -> LegacyTypeInfoProvider.isLT(x.type, state)).map(x -> params.indexOf(x))
                        .map(x -> analysisFromTree(tree.getArguments().get(x), x)).forEach(mthdInvc::addArgs);
            }
            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            if (ofLT) {
                Symbol receiver = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
                if (receiver != null && LegacyTypeInfoProvider.isLT(ASTHelpers.getReceiverType(tree), state))
                    mthdInvc.setReceiver(analysisFromSymbol(receiver));
            }
            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(var1);
        boolean retLt = LegacyTypeInfoProvider.isLT(var1, state);
        boolean paramLT = symb.getParameters().stream().filter(x -> LegacyTypeInfoProvider.isLT(x.type, state)).count() > 0;
        boolean objRefLT = LegacyTypeInfoProvider.isLT(symb.owner.type, state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (retLt || paramLT || objRefLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(mthdInvc.getName() + COLUMN_SEPERATOR + mthdInvc.getOwner() + COLUMN_SEPERATOR + mthdInvc.getSignature());
            if (paramLT)
                var1.getArguments().stream().filter(x -> LegacyTypeInfoProvider.isLT(x, state))
                        .map(x -> analysisFromTree(x)).forEach(mthdInvc::addArgs);
            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        Symbol.VarSymbol symb = ASTHelpers.getSymbol(var1);
        if (LegacyTypeInfoProvider.isLT(var1, state)) {
            Variable.Vrbl.Builder vrbl = Variable.Vrbl.newBuilder();
            vrbl.setName(symb.getQualifiedName().toString())
                    .setKind(symb.getKind().toString())
                    .setType(symb.type.toString())
                    .setOwner(getOwner(symb))
                    .setId(generateVarId(symb));

            if (var1.getInitializer() != null)
                vrbl.setInitializer(analysisFromTree(var1.getInitializer()));

            vrbl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());

            if (symb.getKind().equals(ElementKind.PARAMETER)) {
                Symbol.MethodSymbol methSymb = (Symbol.MethodSymbol) symb.owner;
                vrbl.setOwnerMethodId(generateMethodId(methSymb));
                vrbl.setOwnerClass(methSymb.owner.toString());
            }

            ProtoBuffPersist.write(vrbl, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchAssignment(AssignmentTree var1, VisitorState state) {
        ExpressionTree lhs = var1.getVariable();
        if (lhs.getKind().equals(Tree.Kind.IDENTIFIER) || lhs.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
            try {
                Assignment.Asgn.Builder asgn = Assignment.Asgn.newBuilder();
                asgn.setLhs(analysisFromTree(lhs))
                        .setRhs(analysisFromTree(var1.getExpression()));
                asgn.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                        .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
                ProtoBuffPersist.write(asgn, var1.getKind().toString());
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
        return null;
    }

    @Override
    public Description matchClass(ClassTree classTree, VisitorState state) {
        boolean implementsLt = classTree.getImplementsClause().stream().filter(x -> LegacyTypeInfoProvider.isLT(x, state)).count() > 0;
        boolean isLT = LegacyTypeInfoProvider.isLT(classTree, state);
        if (implementsLt || isLT) {
            Symbol.ClassSymbol symb = ASTHelpers.getSymbol(classTree);
            ClassDecl.clsDcl.Builder clsDcl = ClassDecl.clsDcl.newBuilder();
            clsDcl.setName(getOwner(symb))
                    .setOwner(symb.owner.toString())
                    .setKind(symb.getKind().toString());

            if (isLT) clsDcl.addLTtype(ASTHelpers.getType(classTree).toString());
            else
                clsDcl.addAllLTtype(classTree.getImplementsClause().stream().filter(x -> LegacyTypeInfoProvider.isLT(x, state))
                        .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList()));
            clsDcl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            ProtoBuffPersist.write(clsDcl, classTree.getKind().toString());
        }

        return null;
    }

    public static String getOwner(Symbol symb) {
        return symb.owner.getKind() + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR +
                (symb.owner.getKind().equals(ElementKind.METHOD) || symb.owner.getKind().equals(ElementKind.CONSTRUCTOR) ?
                        symb.owner.owner.getKind() + COLUMN_SEPERATOR + symb.owner.owner.toString() + COLUMN_SEPERATOR : COLUMN_SEPERATOR);
    }

    public static String getMthdDclName(Symbol.MethodSymbol symb) {
        return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
    }

    public static Analysis.anlys.Builder analysisFromTree(Tree tree, Object... options) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();

        if (options.length > 0) a.setIndex((int) options[0]);

        if (tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION)) {
            LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
            a.setKind(lambda.getKind().toString());
            return a;
        } else if (tree.getKind().equals(Tree.Kind.MEMBER_REFERENCE)) {
            MemberReferenceTree mr = (MemberReferenceTree) tree;
            a.setKind(mr.getKind().toString());
            return a;
        } else if (tree.getKind().equals(Tree.Kind.METHOD_INVOCATION) || tree.getKind().equals(Tree.Kind.IDENTIFIER)
                || tree.getKind().equals(Tree.Kind.NEW_CLASS) || tree.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
            Symbol symb = ASTHelpers.getSymbol(tree);
            a.setName(getName(symb));
            a.setKind(symb.getKind().toString());
            a.setId(getName(symb) + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString());
            return a;
        } else if (tree.getKind().equals(Tree.Kind.VARIABLE)) {
            Symbol.VarSymbol symb = ASTHelpers.getSymbol((VariableTree) tree);
            a.setName(getName(symb));
            a.setKind(symb.getKind().toString());
            a.setId(generateVarId(symb));
            return a;
        } else {

            return a.setKind(tree.getKind().toString()).setName(WARNING).setId(WARNING);
        }
    }


    public static String generateMethodId(Symbol.MethodSymbol symb) {

        return getMthdDclName(symb) + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString();
    }

    public static String generateVarId(Symbol.VarSymbol symb) {
        return symb.getQualifiedName().toString() + COLUMN_SEPERATOR + getOwner(symb) + COLUMN_SEPERATOR + symb.type.toString();
    }

    public static Analysis.anlys.Builder analysisFromSymbol(Symbol symb) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();
        a.setName(getName(symb));
        a.setKind(symb.getKind().toString());
        a.setId(getName(symb) + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString());
        return a;

    }


    public static String getName(Symbol symb) {

        return symb.name.toString().equals(CONSTRUCTOR_INIT)
                ? symb.owner.name.toString()
                : symb.getQualifiedName().toString();
    }
}
