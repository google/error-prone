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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
            infoFromSymbol(methodTree).transform(mthdDcl::setId);

            mthdDcl.setReturnType(ASTHelpers.getType(methodTree.getReturnType()) != null ? ASTHelpers.getType(methodTree.getReturnType()).toString() : RTRN_TYPE_NOT_FOUND);

            List<String> super_methods = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().map(x -> x.owner.toString()).collect(collectingAndThen(Collectors.toList(),
                    Collections::unmodifiableList));

            if (super_methods != null && !super_methods.isEmpty()) {
                mthdDcl.setSuperMethodIn(super_methods.get(0));
            }
            mthdDcl.putAllParameters(Collections.unmodifiableMap(params.stream().filter(x -> DataFilter.apply(x, state))
                    .collect(Collectors.toMap(params::indexOf, x -> infoOfTree(x).build()))));

            mthdDcl.addAllModifier(symb.getModifiers().stream().map(x -> x.toString()).collect(collectingAndThen(Collectors.toList(),
                    Collections::unmodifiableList)));

            ProtoBuffPersist.write(mthdDcl, methodTree.getKind().toString());
        }
        return null;
    }

    //TODO: Removing method invocation returning lambda for now.
    // ideally i shud not be capturing it. It could be caught in assignments and method invocations needing paramLT
    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(tree);
        List<Symbol.VarSymbol> params = symb.getParameters();
        boolean paramLT = params.stream().filter(x -> DataFilter.apply(x.type, state)).count() > 0;
        boolean ofLT = DataFilter.apply(ASTHelpers.getReceiverType(tree), state);
        if (paramLT || ofLT) {
            MethodInvocation.Builder mthdInvc = MethodInvocation.newBuilder();
            infoFromSymbol(tree).transform(mthdInvc::setId);

            mthdInvc.putAllArguments(Collections.unmodifiableMap(tree.getArguments().stream().filter(x -> DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> tree.getArguments().indexOf(x), x -> infoOfTree(x).build()))));

            if (ofLT) mthdInvc.setReceiver(infoOfTree(ASTHelpers.getReceiver(tree)));

            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(var1);
        boolean paramMatters = symb.getParameters().stream().filter(x -> DataFilter.apply(x.type, state)).count() > 0;
        if (paramMatters) {
            MethodInvocation.Builder mthdInvc = MethodInvocation.newBuilder();
            infoFromSymbol(var1).transform(mthdInvc::setId);

            mthdInvc.putAllArguments(Collections.unmodifiableMap(var1.getArguments().stream().filter(x -> DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> var1.getArguments().indexOf(x), x -> infoOfTree(x).build()))));


            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        if (DataFilter.apply(var1, state)) {
            Variable.Builder vrbl = Variable.newBuilder();
            infoFromSymbol(var1).transform(vrbl::setId);

            if (var1.getInitializer() != null)
                vrbl.setInitializer(infoOfTree(var1.getInitializer()));

            vrbl.setEnclosingClass(getEnclosingClass(var1));

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
        if (implementsLt || isLT) {
            ClassDeclaration.Builder clsDcl = ClassDeclaration.newBuilder();
            infoFromSymbol(classTree).transform(clsDcl::setId);

            if (isLT) clsDcl.addSuperType(ASTHelpers.getType(classTree).toString());
            else
                clsDcl.addAllSuperType(Collections.unmodifiableList(classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state))
                        .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList())));
            ProtoBuffPersist.write(clsDcl, classTree.getKind().toString());
        }

        return null;
    }

    public static Identification.Builder infoOfTree(Tree tree) {
        return infoFromSymbol(tree).or(
                tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION) ? checksInsideLambda(tree) :
                        Identification.newBuilder().setKind(tree.getKind().toString()));
    }


    private static Identification.Builder checksInsideLambda(Tree tree) {
        //TODO: check if wrapper methods are called upon input parameters
        Identification.Builder id = Identification.newBuilder();
        id.setKind(tree.getKind().toString());
        return id;
    }

    public static Optional<Identification.Builder> infoFromSymbol(Tree tree) {
        try {
            Symbol symb = ASTHelpers.getSymbol(tree);
            Identification.Builder id = Identification.newBuilder();
            id.setName(getName(symb))
                    .setKind(getKindFromTree(tree).or(symb.getKind().toString()))
                    .setOwner(getName(symb.owner))
                    .setType(symb.type.toString());
            return Optional.of(id);
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    private static Optional<String> getKindFromTree(Tree tree) {
        if (tree.getKind().equals(Kind.METHOD_INVOCATION) || tree.getKind().equals(Kind.NEW_CLASS))
            return Optional.of(tree.getKind().toString());
        return Optional.absent();
    }

    public static String getName(Symbol symb) {
        return symb.isConstructor() ? symb.enclClass().toString() : symb.name.toString();
    }

    public static String getEnclosingClass(Tree tree) {
        try {
            return ASTHelpers.getSymbol(tree).enclClass().toString();
        } catch (Exception e) {
            return "";
        }
    }


}
