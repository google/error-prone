package com.google.errorprone.bugpatterns.slf4j;

import static com.google.errorprone.BugPattern.Category.SLF4J;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.isField;
import static com.google.errorprone.matchers.Matchers.not;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.Modifier;

import com.google.common.collect.Ordering;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.Fix;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.VariableTree;

import autovalue.shaded.com.google.common.common.base.Joiner;

@BugPattern(
    name = "DoNotPublishSlf4jLogger",
    summary = "Do not publish Logger field, it should be private",
    category = SLF4J,
    severity = WARNING)
public class DoNotPublishSlf4jLogger extends BugChecker
        implements VariableTreeMatcher {

    private static final long serialVersionUID = 3718668951312958622L;
    private static final Matcher<VariableTree> PRIVATE = new PrivateMatcher();
    private static final Matcher<VariableTree> SLF4J_LOGGER = new LoggerMatcher();
    private static final Joiner JOINER_ON_SPACE = Joiner.on(' ');
    /**
     * Comparator to sort {@link Modifier}. The order is based on styleguide from Google and Open JDK community.
     * @see <a href="https://google.github.io/styleguide/javaguide.html#s4.8.7-modifiers">Google Java Style</a>
     * @see <a href="http://cr.openjdk.java.net/~alundblad/styleguide/index-v6.html#toc-modifiers">Open JDK Java Style Guidelines</a>
     */
    private static final Comparator<Modifier> MODIFIER_COMPARATOR = Ordering.explicit(
            Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE, Modifier.ABSTRACT,
            Modifier.STATIC, Modifier.FINAL, Modifier.TRANSIENT, Modifier.VOLATILE,
            Modifier.DEFAULT, Modifier.SYNCHRONIZED, Modifier.NATIVE, Modifier.STRICTFP);

    private final String lineSeparator = System.lineSeparator();

    @Override
    public Description matchVariable(VariableTree tree, VisitorState state) {
        if (allOf(isField(), SLF4J_LOGGER, not(PRIVATE)).matches(tree, state)) {
            Fix fix = createSuggestedFix(tree.getModifiers());
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

    private Fix createSuggestedFix(ModifiersTree modifiers) {
        StringBuilder replacement = new StringBuilder();
        List<? extends AnnotationTree> annotations = modifiers.getAnnotations();
        for (AnnotationTree annotation : annotations) {
            replacement.append(annotation);
            replacement.append(lineSeparator);
        }
        Set<Modifier> flags = createSuggestedFlags(modifiers);
        replacement.append(stringify(flags));
        return SuggestedFix.builder()
            .replace(modifiers, replacement.toString())
            .build();
    }

    private Set<Modifier> createSuggestedFlags(ModifiersTree modifiers) {
        Set<Modifier> flags = new HashSet<>(modifiers.getFlags());
        flags.add(Modifier.PRIVATE);
        flags.remove(Modifier.PUBLIC);
        flags.remove(Modifier.PROTECTED);
        return flags;
    }

    private String stringify(Set<Modifier> flags) {
        List<Modifier> sortedFlags = new ArrayList<>(flags);
        sortedFlags.sort(MODIFIER_COMPARATOR);
        return JOINER_ON_SPACE.join(sortedFlags);
    }
}
