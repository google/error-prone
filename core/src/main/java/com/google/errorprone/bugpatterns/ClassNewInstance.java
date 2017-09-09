/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.ProvidesFix;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** @author cushon@google.com (Liam Miller-Cushon) */
@BugPattern(
  name = "ClassNewInstance",
  category = JDK,
  summary =
      "Class.newInstance() bypasses exception checking; prefer"
          + " getDeclaredConstructor().newInstance()",
  severity = WARNING,
  tags = StandardTags.FRAGILE_CODE,
  providesFix = ProvidesFix.REQUIRES_HUMAN_ATTENTION
)
public class ClassNewInstance extends BugChecker implements MethodInvocationTreeMatcher {

  private static final Matcher<ExpressionTree> NEW_INSTANCE =
      instanceMethod().onExactClass(Class.class.getName()).named("newInstance");

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!NEW_INSTANCE.matches(tree, state)) {
      return Description.NO_MATCH;
    }
    SuggestedFix.Builder fix = SuggestedFix.builder();
    fix.replace(
        state.getEndPosition(ASTHelpers.getReceiver(tree)),
        state.getEndPosition(tree),
        ".getDeclaredConstructor().newInstance()");
    boolean fixedExceptions = fixExceptions(state, fix);
    if (!fixedExceptions) {
      fixThrows(state, fix);
    }
    return describeMatch(tree, fix.build());
  }

  // if the match occurrs inside the body of a try statement with existing catch clauses
  // update or add a catch block to handle the new exceptions
  private boolean fixExceptions(final VisitorState state, SuggestedFix.Builder fix) {
    TryTree tryTree = null;
    OUTER:
    for (TreePath path = state.getPath(); path != null; path = path.getParentPath()) {
      if (path.getLeaf() instanceof CatchTree) {
        // don't add more catch blocks if newInstance() was called in a catch block
        return false;
      } else if (path.getLeaf() instanceof TryTree
          && !((TryTree) path.getLeaf()).getCatches().isEmpty()) {
        tryTree = (TryTree) path.getLeaf();
        break;
      }
    }
    if (tryTree == null) {
      return false;
    }
    ImmutableMap.Builder<Type, CatchTree> catches = ImmutableMap.builder();
    for (CatchTree c : tryTree.getCatches()) {
      catches.put(ASTHelpers.getType(c.getParameter().getType()), c);
    }
    UnhandledResult<CatchTree> result = unhandled(catches.build(), state);
    if (result.unhandled.isEmpty()) {
      // no fix needed
      return true;
    }
    {
      // if there's an existing multi-catch at the end that handles reflective exceptions,
      // replace all of them with ROE and leave any non-reflective exceptions.
      // earlier catch blocks are left unchanged.
      CatchTree last = Iterables.getLast(tryTree.getCatches());
      Tree lastType = last.getParameter().getType();
      if (lastType.getKind() == Tree.Kind.UNION_TYPE) {
        Type roe = state.getTypeFromString(ReflectiveOperationException.class.getName());
        Set<String> exceptions = new LinkedHashSet<>();
        boolean foundReflective = false;
        for (Tree alternate : ((UnionTypeTree) lastType).getTypeAlternatives()) {
          if (ASTHelpers.isSubtype(ASTHelpers.getType(alternate), roe, state)) {
            foundReflective = true;
            exceptions.add("ReflectiveOperationException");
          } else {
            exceptions.add(state.getSourceForNode(alternate));
          }
        }
        if (foundReflective) {
          fix.replace(lastType, Joiner.on(" | ").join(exceptions));
          return true;
        }
      }
    }
    // check for duplicated catch blocks that handle reflective exceptions exactly the same way,
    // and merge them into a single block that catches ROE
    Set<String> uniq = new HashSet<>();
    for (CatchTree ct : result.handles.values()) {
      uniq.add(state.getSourceForNode(ct.getBlock()));
    }
    // the catch blocks are all unique, append a new fresh one
    if (uniq.size() != 1) {
      CatchTree last = Iterables.getLast(tryTree.getCatches());
      // borrow the variable name of the previous catch variable, in case the naive 'e' conflicts
      // with something in the current scope
      String name = last.getParameter().getName().toString();
      fix.postfixWith(
          last,
          String.format(
              "catch (ReflectiveOperationException %s) {"
                  + " throw new LinkageError(%s.getMessage(), %s); }",
              name, name, name));
      return true;
    }
    // if the catch blocks contain calls to newInstance, don't delete any of them to avoid
    // overlapping fixes
    final AtomicBoolean newInstanceInCatch = new AtomicBoolean(false);
    ((JCTree) result.handles.values().iterator().next())
        .accept(
            new TreeScanner() {
              @Override
              public void visitApply(JCTree.JCMethodInvocation tree) {
                if (NEW_INSTANCE.matches(tree, state)) {
                  newInstanceInCatch.set(true);
                }
              }
            });
    if (newInstanceInCatch.get()) {
      fix.replace(
          Iterables.getLast(result.handles.values()).getParameter().getType(),
          "ReflectiveOperationException");
      return true;
    }
    // otherwise, merge the duplicated catch blocks into a single block that
    // handles ROE
    boolean first = true;
    for (CatchTree ct : result.handles.values()) {
      if (first) {
        fix.replace(ct.getParameter().getType(), "ReflectiveOperationException");
        first = false;
      } else {
        fix.delete(ct);
      }
    }
    return true;
  }

  // if there wasn't a try/catch to add new catch clauses to, update the enclosing
  // method declaration's throws clause to declare the new checked exceptions
  private void fixThrows(VisitorState state, SuggestedFix.Builder fix) {
    MethodTree methodTree = state.findEnclosing(MethodTree.class);
    if (methodTree == null || methodTree.getThrows().isEmpty()) {
      return;
    }
    ImmutableMap.Builder<Type, ExpressionTree> thrown = ImmutableMap.builder();
    for (ExpressionTree e : methodTree.getThrows()) {
      thrown.put(ASTHelpers.getType(e), e);
    }
    UnhandledResult<ExpressionTree> result = unhandled(thrown.build(), state);
    if (result.unhandled.isEmpty()) {
      return;
    }
    List<String> newThrows = new ArrayList<>();
    for (Type handle : result.unhandled) {
      newThrows.add(handle.tsym.getSimpleName().toString());
    }
    Collections.sort(newThrows);
    fix.postfixWith(
        Iterables.getLast(methodTree.getThrows()), ", " + Joiner.on(", ").join(newThrows));
    // the other exceptions are in java.lang
    fix.addImport("java.lang.reflect.InvocationTargetException");
  }

  static class UnhandledResult<T> {
    /** Exceptions thrown by {@link Constructor#newInstance} that were unhandled. */
    final ImmutableSet<Type> unhandled;

    /** Handlers for reflective exceptions (e.g. a throws declaration or catch clause). */
    final ImmutableMap<Type, T> handles;

    UnhandledResult(ImmutableSet<Type> unhandled, ImmutableMap<Type, T> handles) {
      this.unhandled = unhandled;
      this.handles = handles;
    }
  }

  /**
   * Given a map of handled exception types and the trees of those handlers (i.e. catch clauses or
   * method throws clauses), determine which handlers are for reflective exceptions, and whether all
   * exceptions thrown by {#link Constructor#newInstance} are handled.
   */
  private <T> UnhandledResult<T> unhandled(ImmutableMap<Type, T> handles, VisitorState state) {
    LinkedHashSet<Type> toHandle = new LinkedHashSet<>();
    for (Class<?> e :
        Arrays.asList(
            InstantiationException.class,
            IllegalAccessException.class,
            InvocationTargetException.class,
            NoSuchMethodException.class)) {
      Type type = state.getTypeFromString(e.getName());
      if (type != null) {
        toHandle.add(type);
      }
    }
    Type roe = state.getTypeFromString(ReflectiveOperationException.class.getName());
    ImmutableMap.Builder<Type, T> newHandles = ImmutableMap.builder();
    for (Map.Entry<Type, T> entry : handles.entrySet()) {
      Type type = entry.getKey();
      if (ASTHelpers.isSubtype(type, roe, state)) {
        newHandles.put(type, entry.getValue());
      }
      for (Type precise :
          type.isUnion()
              ? ((Type.UnionClassType) type).getAlternativeTypes()
              : Collections.singleton(type)) {
        Iterator<Type> it = toHandle.iterator();
        while (it.hasNext()) {
          if (ASTHelpers.isSubtype(it.next(), precise, state)) {
            it.remove();
          }
        }
      }
    }
    return new UnhandledResult<>(ImmutableSet.copyOf(toHandle), newHandles.build());
  }
}
