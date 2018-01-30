package com.google.errorprone.bugpatterns.refactoringexperiment;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.*;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.*;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.ElementKind;
import java.util.List;
import java.util.stream.Collectors;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Util.*;

@AutoService(BugChecker.class)
@BugPattern(
        name = "ProtoBuffDataGen",
        category = JDK,
        summary = "String formatting inside print method",
        severity = ERROR,
        linkType = CUSTOM,
        link = "example.com/bugpattern/MyCustomCheck"
)
public class ProtoBuffDataGen extends BugChecker implements BugChecker.MethodTreeMatcher, BugChecker.MethodInvocationTreeMatcher, BugChecker.NewClassTreeMatcher,BugChecker.VariableTreeMatcher
                                                        ,BugChecker.AssignmentTreeMatcher,BugChecker.ClassTreeMatcher
{

    @Override
    public Description matchMethod(MethodTree methodTree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(methodTree);
        List<? extends VariableTree> params = methodTree.getParameters();
        boolean paramsMatter = params.stream().filter(x -> Util.isLT(x, state)).collect(Collectors.toList()).size() > 0;
        boolean returnMatter = Util.isLT(methodTree.getReturnType(), state);
        if (paramsMatter || returnMatter) {
            MethodDeclaration.MthdDcl.Builder mthdDcl = MethodDeclaration.MthdDcl.newBuilder();
            mthdDcl.setName(Util.getMthdDclName(symb))
                    .setOwner(symb.owner.toString())
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(generateMethodId(symb));

            mthdDcl.setReturnType(ASTHelpers.getType(methodTree.getReturnType()) != null ? ASTHelpers.getType(methodTree.getReturnType()).toString() : RTRN_TYPE_NOT_FOUND);

            List<String> y = ASTHelpers.findSuperMethods(symb, state.getTypes()).stream().map(x -> x.owner.toString()).collect(Collectors.toList());

            if (y != null && !y.isEmpty()) {
                mthdDcl.setSuperMethodIn(y.get(0));
            }
            params.stream().filter(x -> Util.isLT(x, state)).map(x -> Util.analysisFromTree(x, params.indexOf(x))).forEach(mthdDcl::addParam);
            ProtoBuffPersist.write(mthdDcl, methodTree.getKind().toString());
        }
        return null;
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(tree);
        List<Symbol.VarSymbol> params = symb.getParameters();

        boolean retLt = Util.isLT(tree, state);
        boolean paramLT = params.stream().filter(x -> Util.isLT(x.type, state)).count() > 0;
        boolean ofLT = Util.isLT(symb.owner.type,state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;

        if (retLt || paramLT || ofLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(Util.getName(symb))
                    .setOwner(Util.getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(generateMethodId(symb));

            if(paramLT){
                params.stream().filter(x -> Util.isLT(x.type, state)).map(x -> params.indexOf(x))
                        .map(x -> Util.analysisFromTree(tree.getArguments().get(x),x)).forEach(mthdInvc::addArgs);
            }
            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            if(ofLT) {
                Symbol receiver = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
                if (receiver != null && Util.isLT(ASTHelpers.getReceiverType(tree),state) )
                    mthdInvc.setReceiver(Util.analysisFromSymbol(receiver));
            }
            ProtoBuffPersist.write(mthdInvc, tree.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchNewClass(NewClassTree var1, VisitorState state) {
        Symbol.MethodSymbol symb = ASTHelpers.getSymbol(var1);
        boolean retLt = Util.isLT(var1, state);
        boolean paramLT = symb.getParameters().stream().filter(x -> Util.isLT(x.type, state)).count() > 0;
        boolean objRefLT = Util.isLT(symb.owner.type, state);
        MethodInvocation.MthdInvc.Builder mthdInvc = null;
        if (retLt || paramLT || objRefLT) {
            mthdInvc = MethodInvocation.MthdInvc.newBuilder();
            mthdInvc.setName(Util.getName(symb))
                    .setOwner(Util.getOwner(symb))
                    .setSignature(symb.type.toString())
                    .setKind(symb.getKind().toString())
                    .setId(mthdInvc.getName() + COLUMN_SEPERATOR + mthdInvc.getOwner() + COLUMN_SEPERATOR + mthdInvc.getSignature());
            if (paramLT) var1.getArguments().stream().filter(x -> Util.isLT(x, state))
                    .map(x -> Util.analysisFromTree(x)).forEach(mthdInvc::addArgs);
            mthdInvc.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
            ProtoBuffPersist.write(mthdInvc, var1.getKind().toString());
        }
        return null;
    }


    @Override
    public Description matchVariable(VariableTree var1, VisitorState state) {
        Symbol.VarSymbol symb = ASTHelpers.getSymbol(var1);
        if (Util.isLT(var1, state)) {
            Variable.Vrbl.Builder vrbl = Variable.Vrbl.newBuilder();
            vrbl.setName(symb.getQualifiedName().toString())
                    .setKind(symb.getKind().toString())
                    .setType(symb.type.toString())
                    .setOwner(Util.getOwner(symb))
                    .setId(generateVarId(symb));

            if (var1.getInitializer() != null) vrbl.setInitializer(Util.analysisFromTree(var1.getInitializer()));

            vrbl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                    .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());

            if(symb.getKind().equals(ElementKind.PARAMETER)){
                Symbol.MethodSymbol methSymb = (Symbol.MethodSymbol)symb.owner;
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
                asgn.setLhs(Util.analysisFromTree(lhs))
                        .setRhs(Util.analysisFromTree(var1.getExpression()));
                asgn.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                        .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
                ProtoBuffPersist.write(asgn, var1.getKind().toString());
            }catch(Exception e){
                System.out.println(e.toString());
            }
        }
        return null;
    }

   @Override
   public Description matchClass(ClassTree classTree, VisitorState state) {
       boolean implementsLt = classTree.getImplementsClause().stream().filter(x -> Util.isLT(x, state)).count() > 0;
       boolean isLT = Util.isLT(classTree, state);
       if(implementsLt|| isLT){
           Symbol.ClassSymbol symb = ASTHelpers.getSymbol(classTree);
           ClassDecl.clsDcl.Builder clsDcl = ClassDecl.clsDcl.newBuilder();
           clsDcl.setName(Util.getOwner(symb))
                   .setOwner(symb.owner.toString())
                   .setKind(symb.getKind().toString());

           if(isLT) clsDcl.addLTtype(ASTHelpers.getType(classTree).toString());
           else clsDcl.addAllLTtype(classTree.getImplementsClause().stream().filter(x -> Util.isLT(x, state))
                   .map(x -> ASTHelpers.getType(x).toString()).collect(Collectors.toList()));
           clsDcl.setSrcFile(state.getPath().getCompilationUnit().getSourceFile().getName())
                   .setPckg(state.getPath().getCompilationUnit().getPackageName().toString());
           ProtoBuffPersist.write(clsDcl,classTree.getKind().toString());
       }

       return null;
   }
}
