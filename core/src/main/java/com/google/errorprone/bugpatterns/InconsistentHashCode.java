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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.equalsMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.hashCodeMethodDeclaration;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.ElementKind;

/**
 * Looks for hashCode implementations which are inconsistent with equals.
 *
 * @author ghm@google.com (Graeme Morgan)
 */
@BugPattern(
    name = "InconsistentHashCode",
    summary =
        "Including fields in hashCode which are not compared in equals violates "
            + "the contract of hashCode.",
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE,
    providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION)
public final class InconsistentHashCode extends BugChecker implements ClassTreeMatcher {

  public static final String MESSAGE =
      "hashCode includes the fields %s, which equals does not. Two instances of this class "
          + "could compare equal, but have different hashCodes, which violates the hashCode "
          + "contract.";

  /** Non-static methods that we might expect to see in #hashCode, and allow. */
  private static final Matcher<ExpressionTree> HASH_CODE_METHODS =
      instanceMethod().onDescendantOf("java.lang.Object").named("hashCode").withParameters();

  /** Non-static methods that we might expect to see in #equals, and allow. */
  private static final Matcher<ExpressionTree> EQUALS_METHODS =
      anyOf(
          instanceMethod().onDescendantOf("java.lang.Object").named("getClass"),
          instanceEqualsInvocation());

  @Override
  public Description matchClass(ClassTree tree, VisitorState state) {
    ClassSymbol classSymbol = getSymbol(tree);
    MethodTree equalsDeclaration = null;
    MethodTree hashCodeDeclaration = null;
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) member;
      if (hashCodeMethodDeclaration().matches(methodTree, state)) {
        hashCodeDeclaration = methodTree;
      } else if (equalsMethodDeclaration().matches(methodTree, state)) {
        equalsDeclaration = methodTree;
      }
    }
    if (equalsDeclaration == null || hashCodeDeclaration == null) {
      return Description.NO_MATCH;
    }
    // Build up a map of methods to the fields they access for simple methods, i.e. getters.
    // Not a SetMultimap, because we do want to distinguish between "method was not analyzable" and
    // "method accessed no fields".
    Map<MethodSymbol, ImmutableSet<Symbol>> fieldsByMethod = new HashMap<>();
    for (Tree member : tree.getMembers()) {
      if (!(member instanceof MethodTree)) {
        continue;
      }
      MethodTree methodTree = (MethodTree) member;
      if (!methodTree.equals(equalsDeclaration) && !methodTree.equals(hashCodeDeclaration)) {
        FieldAccessFinder finder = FieldAccessFinder.scanMethod(state, classSymbol, methodTree);
        if (!finder.failed()) {
          fieldsByMethod.put(getSymbol(methodTree), finder.accessedFields());
        }
      }
    }
    FieldAccessFinder equalsScanner =
        FieldAccessFinder.scanMethod(
            state, classSymbol, equalsDeclaration, fieldsByMethod, HASH_CODE_METHODS);
    FieldAccessFinder hashCodeScanner =
        FieldAccessFinder.scanMethod(
            state, classSymbol, hashCodeDeclaration, fieldsByMethod, EQUALS_METHODS);
    if (equalsScanner.failed() || hashCodeScanner.failed()) {
      return Description.NO_MATCH;
    }
    ImmutableSet<Symbol> fieldsInHashCode = hashCodeScanner.accessedFields();
    ImmutableSet<Symbol> fieldsInEquals = equalsScanner.accessedFields();
    Set<Symbol> difference = new HashSet<>(Sets.difference(fieldsInHashCode, fieldsInEquals));
    // Special-case the situation where #hashCode uses a field containing `hash` for memoization.
    difference.removeIf(f -> f.toString().toLowerCase().contains("hash"));
    String message = String.format(MESSAGE, difference);
    // Skip cases where equals and hashCode compare the same fields, or equals compares none (and
    // so is probably checking reference equality).
    return difference.isEmpty() || fieldsInEquals.isEmpty()
        ? Description.NO_MATCH
        : buildDescription(hashCodeDeclaration).setMessage(message).build();
  }

  /**
   * Scans a method to find which fields are accessed from it. Fails if instance methods not
   * matching {@code acceptableMethods} are found: that is, it's optimised for simple getters.
   */
  private static final class FieldAccessFinder extends TreeScanner<Void, Void> {
    private final Matcher<ExpressionTree> acceptableMethods;
    private final Map<MethodSymbol, ImmutableSet<Symbol>> knownMethods;
    private final ImmutableSet.Builder<Symbol> accessedFields = ImmutableSet.builder();
    private final VisitorState state;
    private final ClassSymbol classSymbol;

    // We bail out if we got any unknown method calls to avoid reporting false positives.
    private boolean failed = false;

    private static FieldAccessFinder scanMethod(
        VisitorState state, ClassSymbol classSymbol, MethodTree methodTree) {
      return scanMethod(state, classSymbol, methodTree, ImmutableMap.of(), Matchers.nothing());
    }

    private static FieldAccessFinder scanMethod(
        VisitorState state,
        ClassSymbol classSymbol,
        MethodTree methodTree,
        Map<MethodSymbol, ImmutableSet<Symbol>> knownMethods,
        Matcher<ExpressionTree> acceptableMethods) {
      FieldAccessFinder finder =
          new FieldAccessFinder(state, classSymbol, knownMethods, acceptableMethods);
      methodTree.accept(finder, null);
      return finder;
    }

    private FieldAccessFinder(
        VisitorState state,
        ClassSymbol classSymbol,
        Map<MethodSymbol, ImmutableSet<Symbol>> knownMethods,
        Matcher<ExpressionTree> acceptableMethods) {
      this.state = state;
      this.classSymbol = classSymbol;
      this.knownMethods = knownMethods;
      this.acceptableMethods = acceptableMethods;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
      MethodSymbol symbol = getSymbol(tree);
      if (symbol != null) {
        if (knownMethods.containsKey(symbol)) {
          accessedFields.addAll(knownMethods.get(symbol));
        } else if (!symbol.isStatic() && !acceptableMethods.matches(tree, state)) {
          failed = true;
        }
      }
      return super.visitMethodInvocation(tree, null);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, Void unused) {
      ExpressionTree receiver = getReceiver(tree);
      if (receiver == null || receiver.toString().equals("this")) {
        Symbol symbol = ((JCFieldAccess) tree).sym;
        handleSymbol(symbol);
      }
      return super.visitMemberSelect(tree, null);
    }

    @Override
    public Void visitIdentifier(IdentifierTree tree, Void unused) {
      Symbol symbol = getSymbol(tree);
      handleSymbol(symbol);
      return super.visitIdentifier(tree, null);
    }

    private void handleSymbol(Symbol symbol) {
      if (symbol.getKind() == ElementKind.FIELD
          && !symbol.isStatic()
          && symbol.owner.equals(classSymbol)) {
        String name = symbol.name.toString();
        if (name.equals("this") || name.equals("super")) {
          return;
        }
        accessedFields.add(symbol);
      }
    }

    private ImmutableSet<Symbol> accessedFields() {
      return accessedFields.build();
    }

    public boolean failed() {
      return failed;
    }
  }
}
