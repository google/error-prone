package com.google.errorprone.bugpatterns.refactoringexperiment;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;

import com.google.auto.service.AutoService;
import com.google.common.base.Optional;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
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
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.List;
import java.util.function.Function;
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
            mthdDcl.putAllParam(params.stream().filter(x -> DataFilter.apply(x, state))
                    .collect(Collectors.toMap(x -> params.indexOf(x), x -> generateId(ASTHelpers.getSymbol(x)))));

            mthdDcl.addAllModifier(symb.getModifiers().stream().map(x -> x.toString()).collect(Collectors.toList()));

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
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (paramLT || ofLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(tree.getKind().toString())
                    .setId(generateId(symb));

            mthdInvc.putAllArgs(params.stream().filter(x -> DataFilter.apply(x.type, state))
                    .map(x -> params.indexOf(x))
                    .collect(Collectors.toMap(Function.identity(), x -> infoOfTree(tree.getArguments().get(x)))));

            if (paramLT)
                params.stream().filter(x -> DataFilter.apply(x.type, state)).map(x -> params.indexOf(x))
                        .map(x -> infoOfTree(tree.getArguments().get(x)));

            if (ofLT) mthdInvc.setReceiver(infoOfTree(ASTHelpers.getReceiver(tree)));

            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(var1);
        List<Symbol.VarSymbol> params = symb.getParameters();
        boolean retLt = DataFilter.apply(var1, state);
        boolean paramLT = symb.getParameters().stream().filter(x -> DataFilter.apply(x.type, state)).count() > 0;
        boolean objRefLT = DataFilter.apply(symb.owner.type, state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (retLt || paramLT || objRefLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(getName(symb))
                    .setOwner(getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(var1.getKind().toString())
                    .setId(generateId(symb));

            if (paramLT)
                mthdInvc.putAllArgs(params.stream().filter(x -> DataFilter.apply(x.type, state))
                        .map(x -> params.indexOf(x))
                        .collect(Collectors.toMap(Function.identity(), x -> infoOfTree(var1.getArguments().get(x)))));

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
                vrbl.setInitializer(infoOfTree(var1.getInitializer()));

            ProtoBuffPersist.write(vrbl, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchAssignment(AssignmentTree var1, VisitorState state) {
        ExpressionTree lhs = var1.getVariable();
        Symbol symb = ASTHelpers.getSymbol(var1);
        if (symb!= null && DataFilter.apply(symb.type,state)) {
            Assignment.Asgn.Builder asgn = Assignment.Asgn.newBuilder();
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
            Symbol.ClassSymbol symb = ASTHelpers.getSymbol(classTree);
            ClassDecl.clsDcl.Builder clsDcl = ClassDecl.clsDcl.newBuilder();
            clsDcl.setName(getOwner(symb))
                    .setOwner(symb.owner.toString())
                    .setKind(classTree.getKind().toString());

            if (isLT) clsDcl.addSuperType(ASTHelpers.getType(classTree).toString());
            else
                clsDcl.addAllSuperType(classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state))
                        .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList()));
            ProtoBuffPersist.write(clsDcl, classTree.getKind().toString());
        }

        return null;
    }

    public static String infoOfTree(Tree tree) {
        return infoFromSymbol(tree).or(
                tree.getKind().equals(Tree.Kind.LAMBDA_EXPRESSION) ? checksInsideLambda(tree) :
                        tree.getKind().toString());
    }


    private static String checksInsideLambda(Tree tree) {
        //TODO: check if wrapper methods are called upon input parameters
        return tree.getKind().toString();
    }

    public static Optional<String> infoFromSymbol(Tree tree) {
        try {
            Symbol symb = ASTHelpers.getSymbol(tree);
            return Optional.of(generateId(symb) + COLUMN_SEPERATOR + symb.getKind().toString());
        } catch (Exception e) {
            return Optional.absent();
        }
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



}
