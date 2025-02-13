/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.javadoc;

import static com.google.errorprone.BugPattern.LinkType.CUSTOM;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.bugpatterns.javadoc.Utils.diagnosticPosition;
import static com.google.errorprone.bugpatterns.javadoc.Utils.getDocTreePath;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isEffectivelyPrivate;
import static java.util.stream.Collectors.joining;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.ReturnTree;
import com.sun.source.doctree.SeeTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.DCTree.DCDocComment;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.jspecify.annotations.Nullable;

/**
 * Matches Javadocs which are missing a required summary line.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    summary = "A summary line is required on public/protected Javadocs.",
    severity = WARNING,
    linkType = CUSTOM,
    link = "https://google.github.io/styleguide/javaguide.html#s7.2-summary-fragment",
    documentSuppression = false)
public final class MissingSummary extends BugChecker
    implements ClassTreeMatcher, MethodTreeMatcher, VariableTreeMatcher {

  private static final String CONSIDER_USING_MESSAGE =
      "A summary fragment is required; consider using the value of the @%s block as a "
          + "summary fragment instead.";

  @Override
  public Description matchClass(ClassTree classTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  @Override
  public Description matchMethod(MethodTree methodTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  @Override
  public Description matchVariable(VariableTree variableTree, VisitorState state) {
    return handle(getDocTreePath(state), state);
  }

  private Description handle(@Nullable DocTreePath docTreePath, VisitorState state) {
    if (docTreePath == null) {
      return NO_MATCH;
    }
    if (!requiresJavadoc(docTreePath.getTreePath().getLeaf(), state)) {
      return Description.NO_MATCH;
    }
    List<? extends DocTree> firstSentence = docTreePath.getDocComment().getFirstSentence();
    if (!firstSentence.isEmpty()) {
      return NO_MATCH;
    }
    Symbol symbol = getSymbol(docTreePath.getTreePath().getLeaf());
    if (symbol == null) {
      return NO_MATCH;
    }
    // Skip constructors: a summary line on a constructor doesn't necessarily add a lot of value.
    if (symbol.isConstructor()) {
      return NO_MATCH;
    }
    ReturnTree returnTree = findFirst(docTreePath, ReturnTree.class);
    if (returnTree != null) {
      Description description = generateReturnFix(docTreePath, returnTree, state);
      if (!description.equals(NO_MATCH)) {
        return description;
      }
    }
    SeeTree seeTree = findFirst(docTreePath, SeeTree.class);
    if (seeTree != null) {
      return generateSeeFix(docTreePath, seeTree, state);
    }
    Set<Modifier> modifiers = symbol.getModifiers();
    if (!modifiers.contains(Modifier.PUBLIC) && !modifiers.contains(Modifier.PROTECTED)) {
      return NO_MATCH;
    }
    if (hasAnnotation(symbol, "java.lang.Override", state)
        || hasAnnotation(symbol, "java.lang.Deprecated", state)) {
      return NO_MATCH;
    }
    return buildDescription(diagnosticPosition(docTreePath, state)).build();
  }

  private Description generateReturnFix(
      DocTreePath docTreePath, ReturnTree returnTree, VisitorState state) {
    int pos = ((DCDocComment) docTreePath.getDocComment()).comment.getSourcePos(0);
    String description = returnTree.toString().replaceAll("^@return ", "");
    if (description.isEmpty()) {
      return NO_MATCH;
    }
    SuggestedFix fix =
        Utils.replace(returnTree, "", state).toBuilder()
            .replace(
                pos,
                pos,
                String.format(
                    "Returns %s%s\n",
                    lowerFirstLetter(description), description.endsWith(".") ? "" : "."))
            .build();
    return buildDescription(diagnosticPosition(docTreePath, state))
        .setMessage(String.format(CONSIDER_USING_MESSAGE, "return"))
        .addFix(fix)
        .build();
  }

  private Description generateSeeFix(DocTreePath docTreePath, SeeTree seeTree, VisitorState state) {
    int pos = ((DCDocComment) docTreePath.getDocComment()).comment.getSourcePos(0);
    // javac fails to provide an endpos for @see sometimes; don't emit a fix in that case.
    SuggestedFix replacement = Utils.replace(seeTree, "", state);
    SuggestedFix fix =
        replacement.isEmpty()
            ? replacement
            : replacement.toBuilder()
                .replace(
                    pos,
                    pos,
                    String.format(
                        "See {@link %s}.\n",
                        seeTree.getReference().stream()
                            .map(Object::toString)
                            .collect(joining(" "))))
                .build();
    return buildDescription(diagnosticPosition(docTreePath, state))
        .setMessage(String.format(CONSIDER_USING_MESSAGE, "see"))
        .addFix(fix)
        .build();
  }

  private static <T> @Nullable T findFirst(DocTreePath docTreePath, Class<T> clazz) {
    return new DocTreeScanner<T, Void>() {
      @Override
      public T scan(DocTree docTree, Void unused) {
        if (clazz.isInstance(docTree)) {
          return clazz.cast(docTree);
        }
        return super.scan(docTree, null);
      }
    }.scan(docTreePath.getLeaf(), null);
  }

  private static String lowerFirstLetter(String description) {
    return Character.toLowerCase(description.charAt(0)) + description.substring(1);
  }

  private static boolean requiresJavadoc(Tree tree, VisitorState state) {
    if (state.errorProneOptions().isTestOnlyTarget()) {
      return false;
    }
    Symbol symbol = getSymbol(tree);
    if (symbol instanceof MethodSymbol methodSymbol
        && !findSuperMethods(methodSymbol, state.getTypes()).isEmpty()) {
      return false;
    }
    return symbol != null && !isEffectivelyPrivate(symbol);
  }
}
