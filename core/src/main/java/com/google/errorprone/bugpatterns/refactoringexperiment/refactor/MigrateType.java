package com.google.errorprone.bugpatterns.refactoringexperiment.refactor;


import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.GOOGLE_COMMON_BASE_PREDICATE;
import static com.google.errorprone.bugpatterns.refactoringexperiment.Constants.METHOD_INVOCATION;
import static com.google.errorprone.bugpatterns.refactoringexperiment.IdentificationExtractionUtil.infoFromTree;
import static com.google.errorprone.bugpatterns.refactoringexperiment.analysis.ConstructGraph.isTypeKind;

import com.google.auto.service.AutoService;
import com.google.common.base.Strings;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.refactoringexperiment.DataFilter;
import com.google.errorprone.bugpatterns.refactoringexperiment.analysis.Mapping;
import com.google.errorprone.bugpatterns.refactoringexperiment.analysis.QueryProtoBuffData;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.IdentificationOuterClass.Identification;
import com.google.errorprone.bugpatterns.refactoringexperiment.models.RefactorableOuterClass.Refactorable;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AutoService(BugChecker.class)
@BugPattern(
        name = "MigrateType",
        category = JDK,
        summary = "String formatting inside print method",
        severity = ERROR,
        linkType = CUSTOM,
        link = "example.com/bugpattern/MigrateType"
)

/**
 * Created by ameya on 2/9/18.
 */
public class MigrateType extends BugChecker implements BugChecker.VariableTreeMatcher, BugChecker.MethodInvocationTreeMatcher,BugChecker.ClassTreeMatcher,
        BugChecker.MemberReferenceTreeMatcher {

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (DataFilter.apply(tree, state)) {
            Optional<Refactorable> info = getRefactorInfo(tree);
            if (info.isPresent() && !Strings.isNullOrEmpty(info.get().getRefactorTo())) {
                SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
                fixBuilder.addImport(getImportName(info.get().getRefactorTo()));
                fixBuilder.removeImport(GOOGLE_COMMON_BASE_PREDICATE);
                fixBuilder.replace(tree.getType(), getClassName(getImportName(info.get().getRefactorTo()), tree.getType()));
                return describeMatch(tree, fixBuilder.build());
            }
        }
        return null;
    }

    @Override
    public Description matchClass(ClassTree classTree, VisitorState state) {
        boolean implementsLt = classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state)).count() > 0;
        boolean isLT = DataFilter.apply(classTree, state);
        if (implementsLt || isLT) {
            SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
            changeImplementClauseForClass(classTree, state, fixBuilder);
            return describeMatch(classTree, fixBuilder.build());
        }

        return null;
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (DataFilter.apply(ASTHelpers.getReceiver(tree), state)) {
            Optional<Refactorable> rec = getRefactorInfo(ASTHelpers.getReceiver(tree));
            Optional<Identification> mid = infoFromTree(tree);
            if (rec.isPresent() && mid.isPresent()) {
                Optional<Refactorable> mi = getRefactorInfo(mid.get().toBuilder().setOwner(rec.get().getId()).build());
                return changeNameMethodInvocation(tree, mi);
            }else{
                rec = getRefactorInfoOfType(infoFromTree(ASTHelpers.getReceiver(tree)).get());
                if (rec.isPresent() && mid.isPresent()) {
                    Optional<Refactorable> mi = getRefactorInfo(mid.get().toBuilder().setOwner(infoFromTree(ASTHelpers.getReceiver(tree)).get()).build());
                    return changeNameMethodInvocation(tree, mi);
                }
            }
        }
        return null;
    }


    @Override
    public Description matchMemberReference(MemberReferenceTree tree, VisitorState state) {
        if(DataFilter.apply(tree.getQualifierExpression(),state)){
            Optional<Refactorable> rec = getRefactorInfo(tree.getQualifierExpression());
            Optional<Identification> method_id = infoFromTree(tree).map(x ->x.toBuilder().setKind(METHOD_INVOCATION).build());
            if (rec.isPresent() && method_id.isPresent()) {
                Optional<Refactorable> mthd_invc = getRefactorInfo(method_id.get().toBuilder().setOwner(infoFromTree(tree.getQualifierExpression()).get()).build());
                return changeNameMemberReference(tree, mthd_invc);
            }else{
                rec = getRefactorInfoOfType(infoFromTree(tree.getQualifierExpression()).get());
                if (rec.isPresent() && method_id.isPresent()) {
                    Optional<Refactorable> mthd_invc = getRefactorInfo(method_id.get().toBuilder().setOwner(infoFromTree(tree.getQualifierExpression()).get()).build());
                    return changeNameMemberReference(tree, mthd_invc);
                }
            }
        }
        return null;
    }

    private Description changeNameMemberReference(MemberReferenceTree tree, Optional<Refactorable> mi) {
        if (mi.isPresent()) {
            System.out.println(mi.get().getRefactorTo());
            SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
            fixBuilder.replace(tree,tree.getQualifierExpression() + "::" + mi.get().getRefactorTo() );
            return describeMatch(tree, fixBuilder.build());
        }
        return null;
    }


    private Description changeNameMethodInvocation(MethodInvocationTree tree, Optional<Refactorable> mi) {

        if (mi.isPresent()) {
            Symbol receiverSym = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
            SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
            fixBuilder.replace(tree.getMethodSelect(), receiverSym.name + "." + mi.get().getRefactorTo());
            return describeMatch(tree, fixBuilder.build());
        }
        return null;
    }

    private void changeImplementClauseForClass(ClassTree classTree, VisitorState state, SuggestedFix.Builder fixBuilder) {
        Optional<Refactorable> info = getRefactorInfo(classTree);
        if (info.isPresent()) {
            Tree clauseToEdit = classTree.getImplementsClause().stream().filter(x -> DataFilter.apply(x, state)).findFirst().map(x -> (Tree) x)
                    .orElse(DataFilter.apply(classTree.getExtendsClause(), state) ? classTree.getExtendsClause() : null);
            fixBuilder.addImport(getImportName(info.get().getRefactorTo()));
            fixBuilder.replace(clauseToEdit, getClassName(getImportName(info.get().getRefactorTo()), clauseToEdit));
        }
    }

    private String getImportName(String refactorTo) {
        return refactorTo.contains("<")
                ? refactorTo.substring(0, refactorTo.indexOf("<"))
                : refactorTo;
    }

    private String getClassName(String refactorTo, Tree type) {
        String preservedTypeParam = "";
        if (type.getKind().equals(Kind.PARAMETERIZED_TYPE)) {
            List<? extends Tree> typeArg = ((ParameterizedTypeTree) type).getTypeArguments();
            if (Mapping.PRESERVE_ARG.containsKey(refactorTo)) {
                preservedTypeParam = preserveType(Mapping.PRESERVE_ARG.get(refactorTo).stream()
                        .map(x -> typeArg.get(x).toString()).collect(Collectors.toList()));
            }
        }

        return getImportName(refactorTo).replace("java.util.function.", "") + preservedTypeParam;
    }


    private static Optional<Refactorable> getRefactorInfo(Tree tree) {
        Optional<Identification> id = infoFromTree(tree);
        return id.isPresent() ? getRefactorInfo(id.get()) : Optional.empty();

    }

    private static Optional<Refactorable> getRefactorInfoOfType(Identification id) {
        try {
            List<Refactorable> refactorInfo = QueryProtoBuffData.getAllRefactorInfo("");
            return refactorInfo.stream().filter(x -> isTypeKind.test(x.getId())).filter(x -> x.getId().getType().equals(id.getType())).findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }

    }

    private static Optional<Refactorable> getRefactorInfo(Identification id) {
        try {
            List<Refactorable> refactorInfo = QueryProtoBuffData.getAllRefactorInfo("");
            return refactorInfo.stream().filter(x -> x.getId().equals(id)).findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    private static String preserveType(List<String> typeParameter) {
        StringBuilder preserveType = new StringBuilder("<");
        int counter = 0;
        for (String s : typeParameter) {
            if (counter > 0) {
                preserveType.append("," + s);
            } else {
                preserveType.append(s);
            }
            counter += 1;
        }
        return preserveType.append(">").toString();
    }



}
