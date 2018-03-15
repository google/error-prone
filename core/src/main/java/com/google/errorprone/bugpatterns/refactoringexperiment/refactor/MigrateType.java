package com.google.errorprone.bugpatterns.refactoringexperiment.refactor;


import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.bugpatterns.refactoringexperiment.IdentificationExtractionUtil.infoFromTree;

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
public class MigrateType extends BugChecker implements BugChecker.VariableTreeMatcher, BugChecker.MethodInvocationTreeMatcher {

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (DataFilter.apply(tree, state)) {
            Optional<Refactorable> info = getRefactorInfo(tree);
            if (info.isPresent() && !Strings.isNullOrEmpty(info.get().getRefactorTo())) {
                SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
                fixBuilder.addImport(getImportName(info.get().getRefactorTo()));
                fixBuilder.replace(tree.getType(), getClassName(info.get().getRefactorTo(), tree.getType()));
                return describeMatch(tree, fixBuilder.build());
            }
        }
        return null;
    }

    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (DataFilter.apply(ASTHelpers.getReceiver(tree), state)) {
            Optional<Refactorable> rec = getRefactorInfo(ASTHelpers.getReceiver(tree));
            Optional<Identification> mid = infoFromTree(tree);
            if (rec.isPresent() && mid.isPresent()) {
                Optional<Refactorable> mi = getRefactorInfo(mid.get().toBuilder().setOwner(rec.get().getId()).build());
                if (mi.isPresent()) {
                    Symbol receiverSym = ASTHelpers.getSymbol(ASTHelpers.getReceiver(tree));
                    SuggestedFix.Builder fixBuilder = SuggestedFix.builder();
                    fixBuilder.replace(tree.getMethodSelect(), receiverSym.name + "." + mi.get().getRefactorTo());
                    return describeMatch(tree, fixBuilder.build());
                }
            }
        }
        return null;
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
            if (Mapping.preserveArg.containsKey(refactorTo)) {
                preservedTypeParam = preserveType(Mapping.preserveArg.get(refactorTo).stream()
                        .map(x -> typeArg.get(x).toString()).collect(Collectors.toList()));
            }
        }

        return getImportName(refactorTo).replace("java.util.function.", "") + preservedTypeParam;
    }


    private static Optional<Refactorable> getRefactorInfo(Tree tree) {
        Optional<Identification> id = infoFromTree(tree);
        return id.isPresent() ? getRefactorInfo(id.get()) : Optional.empty();

    }

    private static Optional<Refactorable> getRefactorInfo(Identification id) {
        try {
            List<Refactorable> refactorInfo = QueryProtoBuffData.getAllRefactorInfo("../testProtos/");
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
