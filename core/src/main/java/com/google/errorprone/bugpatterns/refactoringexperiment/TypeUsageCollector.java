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
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
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

    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(methodTree);
        List<? extends VariableTree> params = methodTree.getParameters();
        boolean paramsMatter = params.stream().filter(x -> DataFilter.apply(x, state)).collect(Collectors.toList()).size() > 0;
        boolean returnMatter = DataFilter.apply(methodTree.getReturnType(), state);
        System.out.println(symb.getQualifiedName().toString() + "||" + symb.owner.toString());


        if (paramsMatter || returnMatter) {
            MethodDeclaration.MthdDcl.Builder mthdDcl = MethodDeclaration.MthdDcl.newBuilder();
            mthdDcl.setName(getName(symb))
                    .setOwner(symb.owner.toString())
                    .setSignature(symb.type.toString())
                    .setKind(methodTree.getKind().toString())
                    .setId(generateId(symb));

            mthdDcl.setReturnType(ASTHelpers.getType(methodTree.getReturnType()) != null ? ASTHelpers.getType(methodTree.getReturnType()).toString() : RTRN_TYPE_NOT_FOUND);

            List<String> y = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().map(x -> x.owner.toString()).collect(Collectors.toList());

            if (y != null && !y.isEmpty()) {
                mthdDcl.setSuperMethodIn(y.get(0));
            }

            params.stream().filter(x -> DataFilter.apply(x, state)).map(x -> params.indexOf(x))
                    .map(x -> analysisFromTree(params.get(x), x)).forEach(mthdDcl::addParam);

            mthdDcl.addAllModifier(symb.getModifiers().stream().map(x -> x.toString()).collect(Collectors.toList()));

            ProtoBuffPersist.write(mthdDcl, methodTree.getKind().toString());
        }
        return null;

    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(tree);
        List<Symbol.VarSymbol> params = symb.getParameters();
        boolean retLt = DataFilter.apply(tree, state);
        boolean paramLT = params.stream().filter(x -> DataFilter.apply(x.type, state)).count() > 0;
        boolean ofLT = DataFilter.apply(ASTHelpers.getReceiverType(tree), state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (retLt || paramLT || ofLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(tree.getKind().toString())
                    .setId(generateId(symb));

            if (paramLT)
                params.stream().filter(x -> DataFilter.apply(x.type, state)).map(x -> params.indexOf(x))
                        .map(x -> analysisFromTree(tree.getArguments().get(x), x))
                        .forEach(mthdInvc::addArgs);


            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            if (ofLT) {
                Symbol receiver = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
                if (receiver != null && DataFilter.apply(ASTHelpers.getReceiverType(tree), state))
                    mthdInvc.setReceiver(analysisFromSymbol(receiver));
            }
            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(var1);
        List<Symbol.VarSymbol> params = symb.getParameters();
        boolean retLt = DataFilter.apply(var1, state);
        boolean paramLT = symb.getParameters().stream().filter(x ->DataFilter.apply(x.type, state)).count() > 0;
        boolean objRefLT = DataFilter.apply(symb.owner.type, state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (retLt || paramLT || objRefLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(var1.getKind().toString())
                    .setId(generateId(symb));
            if (paramLT) {
                params.stream().filter(x -> DataFilter.apply(x.type, state)).map(x -> params.indexOf(x))
                        .map(x -> analysisFromTree(var1.getArguments().get(x), x)).forEach(mthdInvc::addArgs);
            }
            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        Symbol.VarSymbol symb = ASTHelpers.getSymbol(var1);
        if (DataFilter.apply(var1, state)) {
            Variable.Vrbl.Builder vrbl = Variable.Vrbl.newBuilder();
            vrbl.setName(symb.getQualifiedName().toString())
                    .setKind(symb.getKind().toString())
                    .setType(symb.type.toString())
                    .setOwner(getOwner(symb))
                    .setId(generateId(symb));

            if (var1.getInitializer() != null)
                vrbl.setInitializer(analysisFromTree(var1.getInitializer()));

            vrbl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());

            if (symb.getKind().equals(ElementKind.PARAMETER)) {
                Symbol.MethodSymbol methSymb = (Symbol.MethodSymbol) symb.owner;
                vrbl.setOwnerMethodId(generateId(methSymb));
                vrbl.setOwnerClass(methSymb.owner.toString());
            }

            ProtoBuffPersist.write(vrbl, var1.getKind().toString());
        }
        return null;
    }

    @Override
    public Description matchAssignment(AssignmentTree var1, VisitorState state) {
        ExpressionTree lhs = var1.getVariable();
        if ((lhs.getKind().equals(Tree.Kind.VARIABLE) || lhs.getKind().equals(Tree.Kind.MEMBER_SELECT)) && DataFilter.apply(lhs, state)) {

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

        boolean implementsLt = classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state)).count() > 0;
        boolean isLT = DataFilter.apply(classTree, state);
        if(implementsLt|| isLT){
            Symbol.ClassSymbol symb = ASTHelpers.getSymbol(classTree);
            ClassDecl.clsDcl.Builder clsDcl = ClassDecl.clsDcl.newBuilder();
            clsDcl.setName(getOwner(symb))
                    .setOwner(symb.owner.toString())
                    .setKind(classTree.getKind().toString());

            if(isLT) clsDcl.addLTtype(ASTHelpers.getType(classTree).toString());
            else clsDcl.addAllLTtype(classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state))
                    .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList()));
            clsDcl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            ProtoBuffPersist.write(clsDcl,classTree.getKind().toString());
        }

        return null;
    }

    public static Analysis.anlys.Builder analysisFromSymbol(Symbol symb) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();
        a.setName(getName(symb));
        a.setKind(symb.getKind().toString());
        a.setId(generateId(symb));
        return a;
    }


    public static Analysis.anlys.Builder analysisFromSymbol(Tree tree) {
        return  analysisFromSymbol(ASTHelpers.getSymbol(tree));
    }

    public static String generateId(Symbol symb) {
        return getName(symb) + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR + symb.type.toString();
    }

    public static String getName(Symbol symb) {
        return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
    }

    public static String getOwner(Symbol symb) {
        return symb.owner.getKind() + COLUMN_SEPERATOR + symb.owner.toString() + COLUMN_SEPERATOR +
                (symb.owner.getKind().equals(ElementKind.METHOD) || symb.owner.getKind().equals(ElementKind.CONSTRUCTOR) ?
                        symb.owner.owner.getKind() + COLUMN_SEPERATOR + symb.owner.owner.toString() + COLUMN_SEPERATOR : COLUMN_SEPERATOR);
    }

    public static boolean canResolveToSymbol(Tree tree){
        return tree.getKind().equals(Tree.Kind.MEMBER_REFERENCE) || tree.getKind().equals(Tree.Kind.METHOD_INVOCATION) || tree.getKind().equals(Tree.Kind.NEW_CLASS)
                || tree.getKind().equals(Tree.Kind.MEMBER_SELECT) || tree.getKind().equals(Kind.IDENTIFIER) || tree.getKind().equals(Tree.Kind.VARIABLE);
    }

    public static Analysis.anlys.Builder analysisFromTree(Tree tree, Object... options) {
        Analysis.anlys.Builder a = Analysis.anlys.newBuilder();
        if(canResolveToSymbol(tree)){
            a = analysisFromSymbol(tree);
        }else if (tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION) || tree.getKind().equals(Kind.NULL_LITERAL) ) {
            //a.addAllParamMethodInvoked()
            //return a;
            a.setKind(tree.getKind().toString());
        }
        else {
            System.out.println("Could not analyse " + tree.getKind().toString());
            a.setName(WARNING).setId(WARNING).setKind(tree.getKind().toString());
        }
        if (options.length > 0) a.setIndex((int) options[0]);
        return a;
    }


}
