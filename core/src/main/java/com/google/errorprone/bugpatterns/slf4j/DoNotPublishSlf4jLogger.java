package com.google.errorprone.bugpatterns.slf4j;

import static com.google.errorprone.BugPattern.Category.SLF4J;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isField;
import static com.google.errorprone.matchers.Matchers.not;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.VariableTree;

import autovalue.shaded.com.google.common.common.base.Joiner;

@BugPattern(
    name = "DoNotPublishSlf4jLogger",
    summary = "Do not publish Logger field, it should be private",
    category = SLF4J,
    severity = WARNING,
    maturity = MATURE)
public class DoNotPublishSlf4jLogger extends BugChecker
        implements VariableTreeMatcher {

    private static final long serialVersionUID = 3718668951312958622L;
    private static final Matcher<VariableTree> PRIVATE = new PrivateMatcher();
    private static final Matcher<VariableTree> SLF4J_LOGGER = new LoggerMatcher();
    private static final Joiner JOINER_ON_SPACE = Joiner.on(' ');

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (allOf(isField(), SLF4J_LOGGER, not(PRIVATE)).matches(tree, state)) {
            Fix fix = createSuggestedFix(tree);
            return Description
                    .builder(tree, "DoNotPublishSlf4jLogger", null, WARNING, "Do not publish Logger field, it should be private")
                    .addFix(fix)
                    .build();
        }
        return Description.NO_MATCH;
    }

    private static final class PrivateMatcher
            implements Matcher<VariableTree> {
        private static final long serialVersionUID = 4297995943793097263L;

        @Override
        public boolean matches(VariableTree tree, VisitorState state) {
            return tree.getModifiers().getFlags().contains(Modifier.PRIVATE);
        }
    }

    private static final class LoggerMatcher implements Matcher<VariableTree> {
        private static final long serialVersionUID = -682327741943438574L;
        private static final String FQN_SLF4J_LOGGER = "org.slf4j.Logger";

        @Override
        public boolean matches(VariableTree tree, VisitorState state) {
            return FQN_SLF4J_LOGGER.equals(ASTHelpers.getSymbol(tree.getType()).toString());
        }
    }

    private Fix createSuggestedFix(VariableTree tree) {
        /**
         * visibility and annotations
         */
        ModifiersTree modifiers = tree.getModifiers();
        Set<Modifier> flags = new HashSet<>(modifiers.getFlags());
        flags.add(Modifier.PRIVATE);
        flags.remove(Modifier.PUBLIC);
        flags.remove(Modifier.PROTECTED);
        StringBuilder replacement = new StringBuilder()
                .append(modifiers.getAnnotations())
                .append(stringify(flags)).append(' ')
                .append(tree.getType()).append(' ')
                .append(tree.getName());
        ExpressionTree initializer = tree.getInitializer();
        if (initializer != null) {
            replacement.append(" = ").append(initializer);
        }
        replacement.append(';');
        return SuggestedFix.builder()
            .replace(tree, replacement.toString())
            .build();
    }

    private String stringify(Set<Modifier> flags) {
        List<Modifier> sortedFlags = new ArrayList<>(flags);
        sortedFlags.sort(new ModifierComparator());
        return JOINER_ON_SPACE.join(sortedFlags);
    }
}
