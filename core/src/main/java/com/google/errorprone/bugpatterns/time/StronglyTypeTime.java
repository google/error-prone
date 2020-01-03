/*
 * Copyright 2019 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.time;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.constructor;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;

/** Flags fields which would be better expressed as time types rather than primitive integers. */
@BugPattern(
    name = "StronglyTypeTime",
    summary =
        "This primitive integral type is only used to construct time types. It would be clearer to"
            + " strongly type the field instead.",
    severity = WARNING)
public final class StronglyTypeTime extends BugChecker implements CompilationUnitTreeMatcher {
  private static final Matcher<ExpressionTree> TIME_FACTORY =
      anyOf(
          // Java time.
          staticMethod()
              .onClass("java.time.Duration")
              .namedAnyOf("ofDays", "ofHours", "ofMillis", "ofMinutes", "ofNanos", "ofSeconds")
              .withParameters("long"),
          staticMethod()
              .onClass("java.time.Instant")
              .namedAnyOf("ofEpochMilli", "ofEpochSecond")
              .withParameters("long"),
          // Proto time.
          staticMethod()
              .onClass("com.google.protobuf.util.Timestamps")
              .namedAnyOf("fromNanos", "fromMicros", "fromMillis", "fromSeconds"),
          staticMethod()
              .onClass("com.google.protobuf.util.Durations")
              .namedAnyOf(
                  "fromNanos",
                  "fromMicros",
                  "fromMillis",
                  "fromSeconds",
                  "fromMinutes",
                  "fromHours",
                  "fromDays"),
          // Joda time.
          staticMethod()
              .onClass("org.joda.time.Duration")
              .namedAnyOf(
                  "millis", "standardSeconds", "standardMinutes", "standardHours", "standardDays")
              .withParameters("long"),
          constructor().forClass("org.joda.time.Instant").withParameters("long"),
          constructor().forClass("org.joda.time.DateTime").withParameters("long"));

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    Map<VarSymbol, VariableTree> fields = findPotentialFields(state);
    SetMultimap<VarSymbol, ExpressionTree> usages = HashMultimap.create();

    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitMemberSelect(MemberSelectTree memberSelectTree, Void unused) {
        handle(memberSelectTree);
        return super.visitMemberSelect(memberSelectTree, null);
      }

      @Override
      public Void visitIdentifier(IdentifierTree identifierTree, Void unused) {
        handle(identifierTree);
        return null;
      }

      private void handle(Tree tree) {
        Symbol symbol = getSymbol(tree);
        if (!fields.containsKey(symbol)) {
          return;
        }
        Tree parent = getCurrentPath().getParentPath().getLeaf();
        if (!(parent instanceof ExpressionTree)
            || !TIME_FACTORY.matches((ExpressionTree) parent, state)) {
          fields.remove(symbol);
          return;
        }
        usages.put((VarSymbol) symbol, (ExpressionTree) parent);
      }
    }.scan(tree, null);

    for (Map.Entry<VarSymbol, VariableTree> entry : fields.entrySet()) {
      state.reportMatch(describeMatch(entry.getValue(), usages.get(entry.getKey()), state));
    }
    return NO_MATCH;
  }

  private Description describeMatch(
      VariableTree variableTree, Set<ExpressionTree> invocationTrees, VisitorState state) {
    if (invocationTrees.stream().map(ASTHelpers::getSymbol).distinct().count() != 1) {
      return NO_MATCH;
    }
    ExpressionTree factory = invocationTrees.iterator().next();
    String newName = createNewName(variableTree.getName().toString());
    SuggestedFix.Builder fix = SuggestedFix.builder();
    Type targetType = getType(factory);
    String typeName = SuggestedFixes.qualifyType(state, fix, targetType);
    fix.replace(
        variableTree,
        String.format(
            "%s %s %s = %s(%s);",
            state.getSourceForNode(variableTree.getModifiers()),
            typeName,
            newName,
            getMethodSelectOrNewClass(factory, state),
            state.getSourceForNode(variableTree.getInitializer())));

    for (ExpressionTree expressionTree : invocationTrees) {
      fix.replace(expressionTree, newName);
    }
    return buildDescription(variableTree)
        .setMessage(
            String.format(
                "This primitive integral type is only used to construct %s instances. It would be"
                    + " clearer to strongly type the field instead.",
                targetType.tsym.getSimpleName()))
        .addFix(fix.build())
        .build();
  }

  private static String getMethodSelectOrNewClass(ExpressionTree tree, VisitorState state) {
    switch (tree.getKind()) {
      case METHOD_INVOCATION:
        return state.getSourceForNode(((MethodInvocationTree) tree).getMethodSelect());
      case NEW_CLASS:
        return "new " + state.getSourceForNode(((NewClassTree) tree).getIdentifier());
      default:
        throw new AssertionError();
    }
  }

  private static final Pattern TIME_UNIT_REMOVER =
      Pattern.compile(
          "((_?IN)?_?(MILLI|NANO|HOUR|DAY|MINUTE|MIN|SECOND|MSEC|NSEC|SEC|_MS|_NS)S?)?$",
          Pattern.CASE_INSENSITIVE);

  /** Tries to strip any time-related suffix off the field name. */
  private static String createNewName(String fieldName) {
    String newName = TIME_UNIT_REMOVER.matcher(fieldName).replaceAll("");
    // Guard against field names that *just* contain the unit. Not much we can do here.
    return newName.isEmpty() ? fieldName : newName;
  }

  /**
   * Finds potential fields that we might want to turn into Durations: (effectively) final integral
   * private fields.
   */
  // TODO(b/147006492): Consider extracting a helper to find all fields that match a Matcher.
  private Map<VarSymbol, VariableTree> findPotentialFields(VisitorState state) {
    Map<VarSymbol, VariableTree> fields = new HashMap<>();
    new TreeScanner<Void, Void>() {
      @Override
      public Void visitClass(ClassTree classTree, Void unused) {
        return isSuppressed(classTree) ? null : super.visitClass(classTree, null);
      }

      @Override
      public Void visitMethod(MethodTree methodTree, Void unused) {
        return isSuppressed(methodTree) ? null : super.visitMethod(methodTree, null);
      }

      @Override
      public Void visitVariable(VariableTree variableTree, Void unused) {
        VarSymbol symbol = getSymbol(variableTree);
        Type type = state.getTypes().unboxedTypeOrType(symbol.type);
        if (symbol.getKind() == ElementKind.FIELD
            && symbol.getModifiers().contains(Modifier.PRIVATE)
            && isConsideredFinal(symbol)
            && variableTree.getInitializer() != null
            && (isSameType(type, state.getSymtab().intType, state)
                || isSameType(type, state.getSymtab().longType, state))
            && !isSuppressed(variableTree)) {
          fields.put(symbol, variableTree);
        }
        return super.visitVariable(variableTree, null);
      }
    }.scan(state.getPath().getCompilationUnit(), null);
    return fields;
  }
}
