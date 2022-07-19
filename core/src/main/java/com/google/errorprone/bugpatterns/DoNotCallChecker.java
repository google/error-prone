/*
 * Copyright 2017 The Error Prone Authors.
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

import static com.google.common.collect.Streams.stream;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.matchers.method.MethodMatchers.staticMethod;
import static com.google.errorprone.util.ASTHelpers.findSuperMethods;
import static com.google.errorprone.util.ASTHelpers.getReceiver;
import static com.google.errorprone.util.ASTHelpers.getSymbol;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.hasAnnotation;
import static com.google.errorprone.util.ASTHelpers.isConsideredFinal;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.MoreAnnotations;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;

/** A {@link BugChecker}; see the associated {@link BugPattern} annotation for details. */
@BugPattern(name = "DoNotCall", summary = "This method should not be called.", severity = ERROR)
public class DoNotCallChecker extends BugChecker
    implements MethodTreeMatcher, CompilationUnitTreeMatcher {
  private final boolean checkNewGetClassMethods;

  public DoNotCallChecker(ErrorProneFlags flags) {
    checkNewGetClassMethods =
        flags.getBoolean("DoNotCallChecker:CheckNewGetClassMethods").orElse(true);
  }

  private static final Matcher<ExpressionTree> STACK_TRACE_ELEMENT_GET_CLASS =
      instanceMethod().onExactClass("java.lang.StackTraceElement").named("getClass");

  private static final Matcher<ExpressionTree> ANY_GET_CLASS =
      instanceMethod().anyClass().named("getClass");

  // If your method cannot be annotated with @DoNotCall (e.g., it's a JDK or thirdparty method),
  // then add it to this Map with an explanation.
  private static final ImmutableMap<Matcher<ExpressionTree>, String> THIRD_PARTY_METHODS =
      ImmutableMap.<Matcher<ExpressionTree>, String>builder()
          .put(
              staticMethod()
                  .onClass("org.junit.Assert")
                  .named("assertEquals")
                  .withParameters("double", "double"),
              "This method always throws java.lang.AssertionError. Use assertEquals("
                  + "expected, actual, delta) to compare floating-point numbers")
          .put(
              staticMethod()
                  .onClass("org.junit.Assert")
                  .named("assertEquals")
                  .withParameters("java.lang.String", "double", "double"),
              "This method always throws java.lang.AssertionError. Use assertEquals("
                  + "String, expected, actual, delta) to compare floating-point numbers")
          .put(
              instanceMethod()
                  .onExactClass("java.lang.Thread")
                  .named("stop")
                  .withParameters("java.lang.Throwable"),
              "Thread.stop(Throwable) always throws an UnsupportedOperationException")
          .put(
              instanceMethod()
                  .onExactClass("java.sql.Date")
                  .namedAnyOf(
                      "getHours",
                      "getMinutes",
                      "getSeconds",
                      "setHours",
                      "setMinutes",
                      "setSeconds"),
              "The hour/minute/second getters and setters on java.sql.Date are guaranteed to throw"
                  + " IllegalArgumentException because java.sql.Date does not have a time"
                  + " component.")
          .put(
              instanceMethod().onExactClass("java.sql.Date").named("toInstant"),
              "sqlDate.toInstant() is not supported. Did you mean to call toLocalDate() instead?")
          .put(
              instanceMethod()
                  .onExactClass("java.sql.Time")
                  .namedAnyOf(
                      "getYear", "getMonth", "getDay", "getDate", "setYear", "setMonth", "setDate"),
              "The year/month/day getters and setters on java.sql.Time are guaranteed to throw"
                  + " IllegalArgumentException because java.sql.Time does not have a date"
                  + " component.")
          .put(
              instanceMethod().onExactClass("java.sql.Time").named("toInstant"),
              "sqlTime.toInstant() is not supported. Did you mean to call toLocalTime() instead?")
          .put(
              instanceMethod()
                  .onExactClass("java.util.concurrent.ThreadLocalRandom")
                  .named("setSeed"),
              "ThreadLocalRandom does not support setting a seed.")
          .put(
              instanceMethod()
                  .onExactClass("java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock")
                  .named("newCondition"),
              "ReadLocks do not support conditions.")
          .put(
              instanceMethod().onExactClass("java.lang.StackTraceElement").named("getClass"),
              "Calling getClass on StackTraceElement returns the Class object for"
                  + " StackTraceElement, you probably meant to retrieve the class containing the"
                  + " execution point represented by this stack trace element using getClassName")
          .put(
              instanceMethod().onExactClass("java.lang.StackWalker").named("getClass"),
              "Calling getClass on StackWalker returns the Class object for StackWalker, you"
                  + " probably meant to retrieve the class containing the execution point"
                  + " represented by this StackWalker using getCallerClass")
          .put(
              instanceMethod().onExactClass("java.lang.StackWalker$StackFrame").named("getClass"),
              "Calling getClass on StackFrame returns the Class object for StackFrame, you probably"
                  + " meant to retrieve the class containing the execution point represented by"
                  + " this StackFrame using getClassName")
          .put(
              instanceMethod().onExactClass("java.lang.reflect.Constructor").named("getClass"),
              "Calling getClass on Constructor returns the Class object for Constructor, you"
                  + " probably meant to retrieve the class containing the constructor represented"
                  + " by this Constructor using getDeclaringClass")
          .put(
              instanceMethod().onExactClass("java.lang.reflect.Field").named("getClass"),
              "Calling getClass on Field returns the Class object for Field, you probably meant to"
                  + " retrieve the class containing the field represented by this Field using"
                  + " getDeclaringClass")
          .put(
              instanceMethod().onExactClass("java.lang.reflect.Method").named("getClass"),
              "Calling getClass on Method returns the Class object for Method, you probably meant"
                  + " to retrieve the class containing the method represented by this Method using"
                  + " getDeclaringClass")
          .put(
              instanceMethod()
                  .onExactClass("java.lang.reflect.ParameterizedType")
                  .named("getClass"),
              "Calling getClass on ParameterizedType returns the Class object for"
                  + " ParameterizedType, you probably meant to retrieve the class containing the"
                  + " method represented by this ParameterizedType using getRawType")
          .put(
              instanceMethod().onExactClass("java.beans.BeanDescriptor").named("getClass"),
              "Calling getClass on BeanDescriptor returns the Class object for BeanDescriptor, you"
                  + " probably meant to retrieve the class described by this BeanDescriptor using"
                  + " getBeanClass")
          .put(
              /*
               * LockInfo has a publicly visible subclass, MonitorInfo. It seems unlikely that
               * anyone is using getClass() in an attempt to distinguish the two. (If anyone is,
               * then it would make more sense to use instanceof, anyway.)
               */
              instanceMethod().onDescendantOf("java.lang.management.LockInfo").named("getClass"),
              "Calling getClass on LockInfo returns the Class object for LockInfo, you probably"
                  + " meant to retrieve the class of the object that is being locked using"
                  + " getClassName")
          /*
           * These methods are part of Guava, but we have to list them in this "thirdparty" section:
           * We can't annotate them with @DoNotCall because we can't override getClass.
           */
          .put(
              instanceMethod()
                  .onExactClass("com.google.common.reflect.ClassPath$ClassInfo")
                  .named("getClass"),
              "Calling getClass on ClassInfo returns the Class object for ClassInfo, you probably"
                  + " meant to retrieve the class described by this ClassInfo using getName or"
                  + " load")
          /*
           * Users of TypeToken have to create a subclass. The static type of their instance is
           * probably often still "TypeToken," but that may change as we see more usage of `var`. So
           * let's check subclasses, too. If anyone defines an overload of getClass on such a
           * subclass, this check will give that person a bad time in one additional way.
           */
          .put(
              instanceMethod()
                  .onDescendantOf("com.google.common.reflect.TypeToken")
                  .named("getClass"),
              "Calling getClass on TypeToken returns the Class object for TypeToken, you probably"
                  + " meant to retrieve the class described by this TypeToken using getRawType")
          .buildOrThrow();

  static final String DO_NOT_CALL = "com.google.errorprone.annotations.DoNotCall";

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    MethodSymbol symbol = getSymbol(tree);
    if (hasAnnotation(tree, DO_NOT_CALL, state)) {
      if (symbol.getModifiers().contains(Modifier.PRIVATE)) {
        return buildDescription(tree)
            .setMessage("A private method that should not be called should simply be removed.")
            .build();
      }
      if (symbol.getModifiers().contains(Modifier.ABSTRACT)) {
        return NO_MATCH;
      }
      if (!ASTHelpers.methodCanBeOverridden(symbol)) {
        return NO_MATCH;
      }
      return buildDescription(tree)
          .setMessage("Methods annotated with @DoNotCall should be final or static.")
          .addFix(
              SuggestedFixes.addModifiers(tree, state, Modifier.FINAL)
                  .orElse(SuggestedFix.emptyFix()))
          .build();
    }
    return findSuperMethods(symbol, state.getTypes()).stream()
        .filter(s -> hasAnnotation(s, DO_NOT_CALL, state))
        .findAny()
        .map(
            s -> {
              String message =
                  String.format(
                      "Method overrides %s in %s which is annotated @DoNotCall,"
                          + " it should also be annotated.",
                      s.getSimpleName(), s.owner.getSimpleName());
              return buildDescription(tree)
                  .setMessage(message)
                  .addFix(
                      SuggestedFix.builder()
                          .addImport(DO_NOT_CALL)
                          .prefixWith(tree, "@DoNotCall ")
                          .build())
                  .build();
            })
        .orElse(NO_MATCH);
  }

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    ImmutableListMultimap<VarSymbol, Type> assignedTypes = getAssignedTypes(state);

    new SuppressibleTreePathScanner<Void, Void>(state) {
      @Override
      public Void visitMethodInvocation(MethodInvocationTree tree, Void unused) {
        handleTree(tree, getSymbol(tree));
        return super.visitMethodInvocation(tree, null);
      }

      @Override
      public Void visitMemberReference(MemberReferenceTree tree, Void unused) {
        handleTree(tree, getSymbol(tree));
        return super.visitMemberReference(tree, null);
      }

      private void handleTree(ExpressionTree tree, MethodSymbol symbol) {
        for (Map.Entry<Matcher<ExpressionTree>, String> matcher : THIRD_PARTY_METHODS.entrySet()) {
          if (matcher.getKey().matches(tree, state)) {
            if (!checkNewGetClassMethods
                && ANY_GET_CLASS.matches(tree, state)
                && !STACK_TRACE_ELEMENT_GET_CLASS.matches(tree, state)) {
              return;
            }
            state.reportMatch(buildDescription(tree).setMessage(matcher.getValue()).build());
            return;
          }
        }
        checkTree(tree, symbol, state);
      }

      private void checkTree(ExpressionTree tree, MethodSymbol sym, VisitorState state) {
        mustNotCall(tree, sym, state).ifPresent(symbol -> handleDoNotCall(tree, symbol, state));
      }

      private void handleDoNotCall(ExpressionTree tree, Symbol symbol, VisitorState state) {
        String doNotCall = getDoNotCallValue(symbol);
        StringBuilder message = new StringBuilder("This method should not be called");
        if (doNotCall.isEmpty()) {
          message.append(", see its documentation for details.");
        } else {
          message.append(": ").append(doNotCall);
        }
        state.reportMatch(buildDescription(tree).setMessage(message.toString()).build());
      }

      private Optional<Symbol> mustNotCall(
          ExpressionTree tree, MethodSymbol sym, VisitorState state) {
        if (hasAnnotation(sym, DO_NOT_CALL, state)) {
          return Optional.of(sym);
        }
        ExpressionTree receiver = getReceiver(tree);
        Symbol receiverSymbol = getSymbol(receiver);
        if (!(receiverSymbol instanceof VarSymbol)) {
          return Optional.empty();
        }
        ImmutableList<Type> assigned = assignedTypes.get((VarSymbol) receiverSymbol);
        if (!assigned.stream().allMatch(t -> isSameType(t, assigned.get(0), state))) {
          return Optional.empty();
        }
        Types types = state.getTypes();
        return assigned.stream()
            .flatMap(
                typeSeen ->
                    types.closure(typeSeen).stream()
                        .flatMap(
                            t ->
                                t.tsym.members() == null
                                    ? Stream.empty()
                                    : stream(t.tsym.members().getSymbolsByName(sym.name)))
                        .filter(
                            symbol ->
                                !sym.isStatic()
                                    && (sym.flags() & Flags.SYNTHETIC) == 0
                                    && hasAnnotation(symbol, DO_NOT_CALL, state)
                                    && symbol.overrides(
                                        sym,
                                        types.erasure(typeSeen).tsym,
                                        types,
                                        /* checkResult= */ true)))
            .findFirst();
      }
    }.scan(state.getPath(), null);
    return NO_MATCH;
  }

  private ImmutableListMultimap<VarSymbol, Type> getAssignedTypes(VisitorState state) {
    ImmutableListMultimap.Builder<VarSymbol, Type> assignedTypes = ImmutableListMultimap.builder();
    new TreePathScanner<Void, Void>() {
      @Override
      public Void visitVariable(VariableTree node, Void unused) {
        VarSymbol symbol = getSymbol(node);
        if (node.getInitializer() != null && isConsideredFinal(symbol)) {
          Type type = getType(node.getInitializer());
          if (type != null) {
            assignedTypes.put(symbol, type);
          }
        }
        return super.visitVariable(node, null);
      }

      @Override
      public Void visitAssignment(AssignmentTree node, Void unused) {
        Symbol assignee = getSymbol(node.getVariable());
        if (assignee instanceof VarSymbol && isConsideredFinal(assignee)) {
          Type type = getType(node.getExpression());
          if (type != null) {
            assignedTypes.put((VarSymbol) assignee, type);
          }
        }
        return super.visitAssignment(node, null);
      }
    }.scan(state.getPath(), null);
    return assignedTypes.build();
  }

  private static String getDoNotCallValue(Symbol symbol) {
    for (Attribute.Compound a : symbol.getRawAttributes()) {
      if (!a.type.tsym.getQualifiedName().contentEquals(DO_NOT_CALL)) {
        continue;
      }
      return MoreAnnotations.getAnnotationValue(a, "value")
          .flatMap(MoreAnnotations::asStringValue)
          .orElse("");
    }
    throw new IllegalStateException();
  }
}
