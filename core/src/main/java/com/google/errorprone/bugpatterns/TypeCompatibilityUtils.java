/*
 * Copyright 2020 The Error Prone Authors.
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

import com.google.auto.value.AutoValue;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.annotation.Nullable;

/**
 * Logical utility methods to answer the question: Are these two types "compatible" with eachother,
 * in the context of an equality check.
 *
 * <p>i.e.: It is possible that an object of one type could be equal to an object of the other type.
 */
public final class TypeCompatibilityUtils {
  private final boolean treatDifferentProtosAsIncomparable;

  public static TypeCompatibilityUtils fromFlags(ErrorProneFlags flags) {
    return new TypeCompatibilityUtils(
        flags.getBoolean("TypeCompatibility:TreatDifferentProtosAsIncomparable").orElse(true));
  }

  public static TypeCompatibilityUtils allOn() {
    return new TypeCompatibilityUtils(/* treatDifferentProtosAsIncomparable= */ true);
  }

  private TypeCompatibilityUtils(boolean treatDifferentProtosAsIncomparable) {
    this.treatDifferentProtosAsIncomparable = treatDifferentProtosAsIncomparable;
  }

  public TypeCompatibilityReport compatibilityOfTypes(
      Type receiverType, Type argumentType, VisitorState state) {
    return compatibilityOfTypes(receiverType, argumentType, typeSet(state), typeSet(state), state);
  }

  private TypeCompatibilityReport compatibilityOfTypes(
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

  private static final ImmutableSet<String> LEAST_UPPER_BOUNDS_TO_IGNORE =
      ImmutableSet.of(
          "com.google.protobuf.MessageOrBuilder",
          "com.google.protobuf.MessageLiteOrBuilder",
          // Collection, Set, and List is unfortunate since List<String> and Set<String> have a lub
          // class of Collection<String>, but Set and List are incompatible with each other due to
          // their own equality declarations. Since they're all interfaces, however, they're
          // technically cast-compatible to each other.
          //
          // We want to disallow equality between these collection sub-interfaces, but *do* want to
          // allow equality between Collection and List. So, here's my attempt to express that
          // cleanly.
          //
          // There are likely other type hierarchies where this situation occurs, but this one is
          // the most common.
          //
          // Here, the LHS and RHS are disjoint collection types (List, Set, Multiset, etc.)
          // (if they were both of one subtype, the lub wouldn't be Collection directly)
          // So consider them incompatible with each other.
          "java.util.Collection");

  private TypeCompatibilityReport leastUpperBoundGenericMismatch(
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

    for (String lubToIgnore : LEAST_UPPER_BOUNDS_TO_IGNORE) {
      if (ASTHelpers.isSameType(lub, state.getTypeFromString(lubToIgnore), state)
          && !ASTHelpers.isSameType(receiverType, lub, state)
          && !ASTHelpers.isSameType(argumentType, lub, state)) {
        return TypeCompatibilityReport.incompatible(receiverType, argumentType);
      }
    }

    return compatibilityReport;
  }

  private TypeCompatibilityReport matchesSubtypeAndIsGenericMismatch(
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
                    || ASTHelpers.isSameType(tp.receiver, receiverType, state)
                    || previousArgumentTypes.contains(tp.argument)
                    || ASTHelpers.isSameType(tp.argument, argumentType, state)))
        .map(
            types -> {
              Set<Type> nextReceiverTypes = typeSet(state);
              nextReceiverTypes.addAll(previousReceiverTypes);
              nextReceiverTypes.add(receiverType);
              Set<Type> nextArgumentTypes = typeSet(state);
              nextArgumentTypes.addAll(previousArgumentTypes);
              nextArgumentTypes.add(argumentType);
              return compatibilityOfTypes(
                  types.receiver, types.argument, nextReceiverTypes, nextArgumentTypes, state);
            })
        .filter(tcr -> !tcr.compatible())
        .findFirst()
        .orElse(TypeCompatibilityReport.createCompatibleReport());
  }

  private static List<Type> typeArgsAsSuper(Type baseType, Type superType, VisitorState state) {
    Type projectedType = state.getTypes().asSuper(baseType, superType.tsym);
    if (projectedType != null) {
      return projectedType.getTypeArguments();
    }
    return new ArrayList<>();
  }

  private static TreeSet<Type> typeSet(VisitorState state) {
    return new TreeSet<>(
        (t1, t2) ->
            state.getTypes().isSameType(t1, t2) ? 0 : t1.toString().compareTo(t2.toString()));
  }

  @AutoValue
  public abstract static class TypeCompatibilityReport {
    public abstract boolean compatible();

    @Nullable
    public abstract Type lhs();

    @Nullable
    public abstract Type rhs();

    static TypeCompatibilityReport createCompatibleReport() {
      return new AutoValue_TypeCompatibilityUtils_TypeCompatibilityReport(true, null, null);
    }

    static TypeCompatibilityReport incompatible(Type lhs, Type rhs) {
      return new AutoValue_TypeCompatibilityUtils_TypeCompatibilityReport(false, lhs, rhs);
    }
  }

  private static final class TypePair {
    final Type receiver;
    final Type argument;

    TypePair(Type receiver, Type argument) {
      this.receiver = receiver;
      this.argument = argument;
    }
  }
}
