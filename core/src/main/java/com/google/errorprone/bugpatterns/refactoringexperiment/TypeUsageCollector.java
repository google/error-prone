package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.util.stream.Collectors.collectingAndThen;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification.Builder;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TreeVisitor;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.PackageSymbol;

import java.util.Collections;
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

    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        boolean paramsMatter = methodTree.getParameters().stream().filter(x -> DataFilter.apply(x, state)).collect(Collectors.toList()).size() > 0;
        boolean returnMatter = DataFilter.apply(methodTree.getReturnType(), state);
        if (paramsMatter || returnMatter) {
            MethodDeclaration.Builder mthdDcl = manageMethodDecl(state, ASTHelpers.getSymbol(methodTree));
            if (returnMatter)
                mthdDcl.setReturnType(DataFilter.getFilteredType(methodTree.getReturnType(), state));

            ProtoBuffPersist.write(mthdDcl, "METHOD");
        }
        return null;
    }

    private MethodDeclaration.Builder manageMethodDecl(VisitorState state, MethodSymbol symb) {
        MethodDeclaration.Builder mthdDcl = MethodDeclaration.newBuilder();
        infoFromSymbol(symb).map(mthdDcl::setId);

        java.util.Optional<MethodSymbol> y = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().findFirst();
        if (y.isPresent())
            mthdDcl.setSuperMethod(manageMethodDecl(state, y.get()));

        mthdDcl.putAllParameters(Collections.unmodifiableMap(symb.getParameters().stream().filter(x -> DataFilter.apply(x.asType(), state))
                .collect(Collectors.toMap(x -> symb.getParameters().indexOf(x), x -> infoFromSymbol(x).get().build()))));

        mthdDcl.addAllModifier(symb.getModifiers().stream().map(x -> x.toString()).collect(collectingAndThen(Collectors.toList(),
                Collections::unmodifiableList)));


        return mthdDcl;
    }

    //TODO: Removing method invocation returning lambda for now.
    //mthod returning lambda shud be caught here because i could have something
    //like : getLambda().apply(8);
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        boolean paramLT = tree.getArguments().stream().filter(x -> DataFilter.apply(x, state))
                .count() > 0;

        boolean ofLT = DataFilter.apply(ASTHelpers.getReceiverType(tree), state);
        boolean returnMatter = DataFilter.apply(ASTHelpers.getReturnType(tree), state);
        if (paramLT || ofLT || returnMatter) {
            MethodInvocation.Builder mthdInvc = MethodInvocation.newBuilder();
            infoFromTree(tree).ifPresent(id -> mthdInvc.setId(id));
            for(ExpressionTree arg : tree.getArguments())
                if(DataFilter.apply(arg,state))
//                    mthdInvc.putArguments(tree.getArguments().indexOf(arg),Identifications.newBuilder().addAllId(infoOfTree(arg)).build());
                    mthdInvc.putArguments(tree.getArguments().indexOf(arg),infoOfTree(arg)).build();
            if (ofLT)
                infoFromTree(ASTHelpers.getReceiver(tree)).ifPresent(id -> mthdInvc.setReceiver(id));
            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());

        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        boolean paramMatters = var1.getArguments().stream().filter(x -> DataFilter.apply(x, state))
                .count() > 0;
        if (paramMatters) {
            MethodInvocation.Builder mthdInvc = MethodInvocation.newBuilder();
            infoFromTree(var1).ifPresent(id -> mthdInvc.setId(id));
            for(ExpressionTree arg : var1.getArguments())
                if(DataFilter.apply(arg,state))
                    // mthdInvc.putArguments(var1.getArguments().indexOf(arg),Identifications.newBuilder().addAllId(infoOfTree(arg)).build());
                    mthdInvc.putArguments(var1.getArguments().indexOf(arg),infoOfTree(arg)).build();
            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }




    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        if (DataFilter.apply(var1, state)) {
            Variable.Builder vrbl = Variable.newBuilder();
            infoFromTree(var1).map(id -> vrbl.setId(id));
            if (var1.getInitializer() != null)
                vrbl.setInitializer(infoOfTree(var1.getInitializer()));
            vrbl.setFilteredType(DataFilter.getFilteredType(var1, state));
            ProtoBuffPersist.write(vrbl, var1.getKind().toString());
        }
        return null;
    }

    @Override
    public Description matchAssignment(AssignmentTree var1, VisitorState state) {
        ExpressionTree lhs = var1.getVariable();
        if ((lhs.getKind().equals(Tree.Kind.IDENTIFIER) || lhs.getKind().equals(Tree.Kind.MEMBER_SELECT)
                || lhs.getKind().equals(Kind.VARIABLE)) && DataFilter.apply(ASTHelpers.getType(var1), state)) {
            Assignment.Builder asgn = Assignment.newBuilder();
            infoFromTree(lhs).ifPresent(x -> asgn.setLhs(x));
            //asgn.addAllRhs(infoOfTree(var1.getExpression()));
            asgn.setRhs(infoOfTree(var1.getExpression()));
            ProtoBuffPersist.write(asgn, var1.getKind().toString());
        }

        return null;
    }

    @Override
    public Description matchClass(ClassTree classTree, VisitorState state) {
        boolean implementsLt = classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state)).count() > 0;
        boolean isLT = DataFilter.apply(classTree, state);
        ClassDeclaration.Builder clsDcl = ClassDeclaration.newBuilder();
        if ((implementsLt || isLT)) {
            infoFromTree(classTree).map(id -> clsDcl.setId(id));
            clsDcl.setSuperType(DataFilter.getFilteredType(classTree, state));
            ProtoBuffPersist.write(clsDcl, classTree.getKind().toString());

        }
        return null;
    }

    public static Identification infoOfTree(Tree tree) {
        Builder infor = infoFromTree(tree).orElse(null);
        if (infor == null)
            if (tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION))
                infor = checksInsideLambda(tree);
            else
                infor = Identification.newBuilder().setKind(tree.getKind().toString());

        return infor.build();
    }

    private static Identification.Builder checksInsideLambda(Tree tree) {
        LambdaExpressionTree lambda = (LambdaExpressionTree) tree;
        Identification.Builder id = Identification.newBuilder();
        id.setKind(tree.getKind().toString())
                .setType(ASTHelpers.getType(lambda).toString());
        return id;
    }

    public static java.util.Optional<Builder> infoFromTree(Tree tree) {
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

    public static java.util.Optional<Builder> infoFromSymbol(Symbol symb) {
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

    public static Identification getASTOwner(Symbol symb) {

        if (symb.owner.getKind().equals(ElementKind.PACKAGE)) {
            PackageSymbol pkgSymb = (PackageSymbol) symb.owner;
            return Identification.newBuilder().setName(pkgSymb.fullname.toString())
                    .setKind(ElementKind.PACKAGE.toString()).build();
        }

        return infoFromSymbol(symb.owner).get()
                .build();
    }

    public static java.util.Optional<String> getKindFromTree(Tree tree) {
        if (tree.getKind().equals(Kind.METHOD_INVOCATION) || tree.getKind().equals(Kind.NEW_CLASS))
            return java.util.Optional.of(tree.getKind().toString());
        return java.util.Optional.empty();
    }


    public static String getName(Symbol symb) {
        if (symb.name != null)
            return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
        else
            return "";
    }
    public static TreeVisitor<Identification, Void> returnVisitor = new TreeScanner<Identification, Void>() {
        @Override
        public Identification visitReturn(ReturnTree ret, Void v) {
            return infoOfTree(ret.getExpression());
        }
    };


}
