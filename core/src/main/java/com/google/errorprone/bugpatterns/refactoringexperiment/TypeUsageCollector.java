package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static java.util.stream.Collectors.collectingAndThen;

import com.google.auto.service.AutoService;
import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.AssignmentOuterClass.Assignment;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.ClassDeclarationOuterClass.ClassDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Owner;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodDeclarationOuterClass.MethodDeclaration;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.MethodInvocationOuterClass.MethodInvocation;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.VariableOuterClass.Variable;
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
import com.sun.tools.javac.code.Symbol.PackageSymbol;

import java.util.Collections;
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

    public static final String RTRN_TYPE_NOT_FOUND = "RTRN_TYPE_NOT_FOUND";
    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(methodTree);
        List<? extends VariableTree> params = methodTree.getParameters();
        boolean paramsMatter = params.stream().filter(x -> DataFilter.apply(x, state)).collect(Collectors.toList()).size() > 0;
        boolean returnMatter = DataFilter.apply(methodTree.getReturnType(), state);
        if (paramsMatter || returnMatter) {
            MethodDeclaration.Builder mthdDcl = MethodDeclaration.newBuilder();
            infoFromTree(methodTree).transform(mthdDcl::setId);
            mthdDcl.setReturnType(ASTHelpers.getType(methodTree.getReturnType()) != null ? ASTHelpers.getType(methodTree.getReturnType()).toString() : RTRN_TYPE_NOT_FOUND);

            List<String> y = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().map(x -> x.owner.toString()).collect(collectingAndThen(Collectors.toList(),
                    Collections::unmodifiableList));

            if (!y.isEmpty())
                mthdDcl.setSuperMethodIn(y.get(0));

            mthdDcl.putAllParameters(Collections.unmodifiableMap(params.stream().filter(x -> DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> params.indexOf(x), x -> infoOfTree(x).build()))));

            mthdDcl.addAllModifier(symb.getModifiers().stream().map(x -> x.toString()).collect(collectingAndThen(Collectors.toList(),
                    Collections::unmodifiableList)));

            ProtoBuffPersist.write(mthdDcl, methodTree.getKind().toString());
        }
        return null;
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
            infoFromTree(tree).transform(id -> mthdInvc.setId(id));
            mthdInvc.putAllArguments(Collections.unmodifiableMap(tree.getArguments().stream().filter(x ->DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> tree.getArguments().indexOf(x), x -> infoOfTree(x).build()))));

            if (ofLT) {
                mthdInvc.setReceiver(infoOfTree(ASTHelpers.getReceiver(tree)));
            }
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
            infoFromTree(var1).transform(id -> mthdInvc.setId(id));

            mthdInvc.putAllArguments(Collections.unmodifiableMap(var1.getArguments().stream().filter(x ->DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> var1.getArguments().indexOf(x), x -> infoOfTree(x).build()))));

            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        if (DataFilter.apply(var1, state)) {
            Variable.Builder vrbl = Variable.newBuilder();
            infoFromTree(var1).transform(id -> vrbl.setId(id));
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
            asgn.setLhs(infoOfTree(lhs))
                    .setRhs(infoOfTree(var1.getExpression()));
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
            infoFromTree(classTree).transform(id -> clsDcl.setId(id));
            if (isLT) clsDcl.addSuperType(ASTHelpers.getType(classTree).toString());
            else
                clsDcl.addAllSuperType(Collections.unmodifiableList(classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state))
                        .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList())));

            ProtoBuffPersist.write(clsDcl, classTree.getKind().toString());
        }
        return null;
    }

    public static Identification.Builder infoOfTree(Tree tree) {
        return infoFromTree(tree).or(
                tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION) ? checksInsideLambda(tree) :
                        Identification.newBuilder().setKind(tree.getKind().toString()));
    }


    private static Identification.Builder checksInsideLambda(Tree tree) {
        //TODO: check if wrapper methods are called upon input parameters
        Identification.Builder id = Identification.newBuilder();
        id.setKind(tree.getKind().toString());
        return id;
    }

    public static Optional<Identification.Builder> infoFromTree(Tree tree) {
        try {
            Symbol symb = ASTHelpers.getSymbol(tree);
            Identification.Builder id = Identification.newBuilder();
            id.setName(getName(symb))
                    .setKind(getKindFromTree(tree).or(symb.getKind().toString()))
                    .setOwner(getOwner(symb))
                    .setType(symb.type.toString());
            return Optional.of(id);
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    public static Optional<Identification.Builder> infoFromSymbol(Symbol symb) {
        try {
            Identification.Builder id = Identification.newBuilder();
            id.setName(getName(symb))
                    .setKind((symb.getKind().toString()))
                    .setOwner(getOwner(symb))
                    .setType(symb.type.toString());
            return Optional.of(id);
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    private static Owner getOwner(Symbol symb) {

        if (symb.owner.getKind().equals(ElementKind.PACKAGE)) {
            PackageSymbol pkgSymb = (PackageSymbol) symb.owner;
            return Owner.newBuilder()
                    .setId(Identification.newBuilder(Identification.newBuilder().setName(pkgSymb.fullname.toString())
                            .setKind(ElementKind.PACKAGE.toString()).build())).build();
        }

        return Owner.newBuilder()
                .setId(Identification.newBuilder(infoFromSymbol(symb.owner).get()
                        .build())).build();
    }

    private static Optional<String> getKindFromTree(Tree tree) {
        if (tree.getKind().equals(Kind.METHOD_INVOCATION) || tree.getKind().equals(Kind.NEW_CLASS))
            return Optional.of(tree.getKind().toString());
        return Optional.absent();
    }


    public static String getName(Symbol symb) {
        if (symb.name != null)
            return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
        else
            return "";
    }

}
