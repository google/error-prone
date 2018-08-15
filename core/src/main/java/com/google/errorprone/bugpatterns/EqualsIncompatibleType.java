/*
 * Copyright 2015 The Error Prone Authors.
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
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.staticEqualsInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static com.google.errorprone.matchers.Matchers.toType;

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.Signatures;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** @author avenet@google.com (Arnaud J. Venet) */
@BugPattern(
    name = "EqualsIncompatibleType",
    summary = "An equality test between objects with incompatible types always returns false",
    category = JDK,
    severity = WARNING)
public class EqualsIncompatibleType extends BugChecker implements MethodInvocationTreeMatcher {
  private static final Matcher<MethodInvocationTree> STATIC_EQUALS_MATCHER =
      staticEqualsInvocation();

  private static final Matcher<ExpressionTree> INSTANCE_EQUALS_MATCHER = instanceEqualsInvocation();

  private static final Matcher<Tree> ASSERT_FALSE_MATCHER =
      toType(
          MethodInvocationTree.class,
          anyOf(
              instanceMethod().anyClass().named("assertFalse"),
              staticMethod().anyClass().named("assertFalse")));

  @Override
  public Description matchMethodInvocation(
      MethodInvocationTree invocationTree, final VisitorState state) {
    if (!STATIC_EQUALS_MATCHER.matches(invocationTree, state)
        && !INSTANCE_EQUALS_MATCHER.matches(invocationTree, state)) {
      return Description.NO_MATCH;
    }

    // This is the type of the object on which the java.lang.Object.equals() method
    // is called, either directly or indirectly via a static utility method. In the latter,
    // it is the type of the first argument to the static method.
    Type receiverType;
    // This is the type of the argument to the java.lang.Object.equals() method.
    // In case a static utility method is used, it is the type of the second argument
    // to this method.
    Type argumentType;

    if (STATIC_EQUALS_MATCHER.matches(invocationTree, state)) {
      receiverType = ASTHelpers.getType(invocationTree.getArguments().get(0));
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(1));
    } else {
      receiverType = ASTHelpers.getReceiverType(invocationTree);
      argumentType = ASTHelpers.getType(invocationTree.getArguments().get(0));
    }

    TypeCompatibilityReport compatibilityReport =
        compatibilityOfTypes(receiverType, argumentType, state);
    if (compatibilityReport.compatible()) {
      return Description.NO_MATCH;
    }

    // Ignore callsites wrapped inside assertFalse:
    // assertFalse(objOfReceiverType.equals(objOfArgumentType))
    if (ASSERT_FALSE_MATCHER.matches(state.getPath().getParentPath().getLeaf(), state)) {
      return Description.NO_MATCH;
    }

    // When we reach this point, we know that the two following facts hold:
    // (1) The types of the receiver and the argument to the eventual invocation of
    //     java.lang.Object.equals() are incompatible.
    // (2) No common superclass (other than java.lang.Object) or interface of the receiver and the
    //     argument defines an override of java.lang.Object.equals().
    // This equality test almost certainly evaluates to false, which is very unlikely to be the
    // programmer's intent. Hence, this is reported as an error. There is no sensible fix to suggest
    // in this situation.
    return buildDescription(invocationTree)
        .setMessage(
            getMessage(
                invocationTree,
                receiverType,
                argumentType,
                compatibilityReport.lhs(),
                compatibilityReport.rhs(),
                state))
        .build();
  }

  public static TypeCompatibilityReport compatibilityOfTypes(
      Type receiverType, Type argumentType, VisitorState state) {
    return compatibilityOfTypes(
        receiverType, argumentType, new HashSet<>(), new HashSet<>(), state);
  }

  public static TypeCompatibilityReport compatibilityOfTypes(
      Type receiverType,
      Type argumentType,
      Set<Type> previousReceiverTypes,
      Set<Type> previousArgumentTypes,
      VisitorState state) {

    if (receiverType == null || argumentType == null) {
      return TypeCompatibilityReport.createCompatibleReport();
    }
    if (receiverType.isPrimitive()
        && argumentType.isPrimitive()
        && !ASTHelpers.isSameType(receiverType, argumentType, state)) {
      return TypeCompatibilityReport.incompatible(receiverType, argumentType);
    }

    // If one type can be cast into the other, we don't flag the equality test.
    // Note: we do this precisely in this order to allow primitive values to be checked pre-1.7:
    // 1.6: java.lang.Object can't be cast to primitives
    // 1.7: java.lang.Object can be cast to primitives (implicitly through the boxed primitive type)
    if (ASTHelpers.isCastable(argumentType, receiverType, state)) {
      return leastUpperBoundGenericMismatch(
          receiverType, argumentType, previousReceiverTypes, previousArgumentTypes, state);
    }

    // Otherwise, we explore the superclasses of the receiver type as well as the interfaces it
    // implements and we collect all overrides of java.lang.Object.equals(). If one of those
    // overrides is inherited by the argument, then we don't flag the equality test.
    Types types = state.getTypes();
    Predicate<MethodSymbol> equalsPredicate =
        methodSymbol ->
            !methodSymbol.isStatic()
                && ((methodSymbol.flags() & Flags.SYNTHETIC) == 0)
                && types.isSameType(methodSymbol.getReturnType(), state.getSymtab().booleanType)
                && methodSymbol.getParameters().size() == 1
                && types.isSameType(
                    methodSymbol.getParameters().get(0).type, state.getSymtab().objectType);
    Set<MethodSymbol> overridesOfEquals =
        ASTHelpers.findMatchingMethods(
            state.getName("equals"), equalsPredicate, receiverType, types);
    Symbol argumentClass = ASTHelpers.getUpperBound(argumentType, state.getTypes()).tsym;

    for (MethodSymbol method : overridesOfEquals) {
      ClassSymbol methodClass = method.enclClass();
      if (argumentClass.isSubClass(methodClass, types)
          && !methodClass.equals(state.getSymtab().objectType.tsym)
          && !methodClass.equals(state.getSymtab().enumSym)) {
        // The type of the argument shares a superclass
        // (other then java.lang.Object or java.lang.Enum) or interface
        // with the receiver that implements an override of java.lang.Object.equals().

        // These should be compatible, but check any generic types for their compatbilities.
        return leastUpperBoundGenericMismatch(
            receiverType, argumentType, previousReceiverTypes, previousArgumentTypes, state);
      }
    }
    return TypeCompatibilityReport.incompatible(receiverType, argumentType);
  }

  private static TypeCompatibilityReport leastUpperBoundGenericMismatch(
      Type receiverType,
      Type argumentType,
      Set<Type> previousReceiverTypes,
      Set<Type> previousArgumentTypes,
      VisitorState state) {

    // Now, see if we can find a generic superclass between the two types, and if so, check the
    // generic parameters for cast-compatibility:

    // class Super<T> (with an equals() override)
    // class Bar extends Super<String>
    // class Foo extends Super<Integer>
    // Bar and Foo would least-upper-bound to Super, and we compare String and Integer to eachother
    Type lub = state.getTypes().lub(argumentType, receiverType);
    // primitives, etc. can't have a common superclass.
    if (lub.getTag().equals(TypeTag.BOT) || lub.getTag().equals(TypeTag.ERROR)) {
      return TypeCompatibilityReport.createCompatibleReport();
    }

    TypeCompatibilityReport compatibilityReport =
        matchesSubtypeAndIsGenericMismatch(
            receiverType, argumentType, lub, previousReceiverTypes, previousArgumentTypes, state);
    if (!compatibilityReport.compatible()) {
      return compatibilityReport;
    }

    // Collection, Set, and List is unfortunate since List<String> and Set<String> have a lub class
    // of Collection<String>, but Set and List are incompatible with eachother due to their own
    // equality declarations. Since they're all interfaces, however, they're technically
    // cast-compatible to eachother.
    //
    // We want to disallow equality between these collection sub-interfaces, but *do* want to
    // allow equality between Collection and List. So, here's my attempt to express that cleanly.
    //
    // There are likely other type hierarchies where this situation occurs, but this one is the
    // most common.
    Type collectionType = state.getTypeFromString("java.util.Collection");
    if (ASTHelpers.isSameType(lub, collectionType, state)
        && !ASTHelpers.isSameType(receiverType, collectionType, state)
        && !ASTHelpers.isSameType(argumentType, collectionType, state)) {
      // Here, the LHS and RHS are disjoint collection types (List, Set, Multiset, etc.)
      // (if they were both of one subtype, the lub wouldn't be Collection directly)
      // So consider them incompatible with eachother.
      return TypeCompatibilityReport.incompatible(receiverType, argumentType);
    }

    return compatibilityReport;
  }

  private static TypeCompatibilityReport matchesSubtypeAndIsGenericMismatch(
      Type receiverType,
      Type argumentType,
      Type superType,
      Set<Type> previousReceiverTypes,
      Set<Type> previousArgumentTypes,
      VisitorState state) {
    List<Type> receiverTypes = typeArgsAsSuper(receiverType, superType, state);
    List<Type> argumentTypes = typeArgsAsSuper(argumentType, superType, state);

    return Streams.zip(receiverTypes.stream(), argumentTypes.stream(), TypePair::new)
        // If we encounter an f-bound, skip that index's type when comparing the compatibility of
        // types to avoid infinite recursion:
        // interface Super<A extends Super<A, B>, B>
        // class Foo extends Super<Foo, String>
        // class Bar extends Super<Bar, Integer>
        .filter(
            tp ->
                !(previousReceiverTypes.contains(tp.receiver)
                    || tp.receiver.equals(receiverType)
                    || previousArgumentTypes.contains(tp.argument)
                    || tp.argument.equals(argumentType)))
        .map(
            types ->
                compatibilityOfTypes(
                    types.receiver,
                    types.argument,
                    Sets.union(previousReceiverTypes, ImmutableSet.of(receiverType)),
                    Sets.union(previousArgumentTypes, ImmutableSet.of(argumentType)),
                    state))
        .filter(tcr -> !tcr.compatible())
        .findFirst()
        .orElse(TypeCompatibilityReport.createCompatibleReport());
  }

  private static final class TypePair {
    final Type receiver;
    final Type argument;

    TypePair(Type receiver, Type argument) {
      this.receiver = receiver;
      this.argument = argument;
    }
  }

  private static List<Type> typeArgsAsSuper(Type baseType, Type superType, VisitorState state) {
    Type projectedType = state.getTypes().asSuper(baseType, superType.tsym);
    if (projectedType != null) {
      return projectedType.getTypeArguments();
    }
    return new ArrayList<>();
  }

  private static String getMessage(
      MethodInvocationTree invocationTree,
      Type receiverType,
      Type argumentType,
      Type conflictingReceiverType,
      Type conflictingArgumentType,
      VisitorState state) {
    TypeStringPair typeStringPair = new TypeStringPair(receiverType, argumentType);
    String baseMessage =
        "Calling "
            + ASTHelpers.getSymbol(invocationTree).getSimpleName()
            + " on incompatible types "
            + typeStringPair.getReceiverTypeString()
            + " and "
            + typeStringPair.getArgumentTypeString();

    // If receiver/argument are incompatible due to a conflict in the generic type, message that out
    if (!state.getTypes().isSameType(receiverType, conflictingReceiverType)) {
      TypeStringPair conflictingTypes =
          new TypeStringPair(conflictingReceiverType, conflictingArgumentType);
      baseMessage +=
          ". They are incompatible because "
              + conflictingTypes.getReceiverTypeString()
              + " and "
              + conflictingTypes.getArgumentTypeString()
              + " are incompatible.";
    }
    return baseMessage;
  }

  @AutoValue
  public abstract static class TypeCompatibilityReport {
    public abstract boolean compatible();

    @Nullable
    public abstract Type lhs();

    @Nullable
    public abstract Type rhs();

    static TypeCompatibilityReport createCompatibleReport() {
      return new AutoValue_EqualsIncompatibleType_TypeCompatibilityReport(true, null, null);
    }

    static TypeCompatibilityReport incompatible(Type lhs, Type rhs) {
      return new AutoValue_EqualsIncompatibleType_TypeCompatibilityReport(false, lhs, rhs);
    }
  }

  public static class TypeStringPair {
    private String receiverTypeString;
    private String argumentTypeString;

    public TypeStringPair(Type receiverType, Type argumentType) {
      receiverTypeString = Signatures.prettyType(receiverType);
      argumentTypeString = Signatures.prettyType(argumentType);
      if (argumentTypeString.equals(receiverTypeString)) {
        receiverTypeString = receiverType.toString();
        argumentTypeString = argumentType.toString();
      }
    }

    public String getReceiverTypeString() {
      return receiverTypeString;
    }

    public String getArgumentTypeString() {
      return argumentTypeString;
    }
  }
}
