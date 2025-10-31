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

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.errorprone.util.ASTHelpers.enclosingPackage;
import static com.google.errorprone.util.ASTHelpers.findMatchingMethods;
import static com.google.errorprone.util.ASTHelpers.getUpperBound;
import static com.google.errorprone.util.ASTHelpers.isCastable;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Streams;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.MethodSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.code.Types;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.lang.model.type.TypeKind;
import org.jspecify.annotations.Nullable;

/**
 * Methods to answer the question: are these two types "compatible" with each other, in the context
 * of an equality check?
 *
 * <p>That is, based on the types alone, is it possible for them to be equal. In general this
 * requires a common superclass that overrides {@link Object#equals(Object)}, but there are
 * complexities and special-cases.
 */
public final class TypeCompatibility {
  private static final String WITHOUT_EQUALS_REASON =
      ". Though these types are the same, the type doesn't implement equals.";
  private final boolean treatBuildersAsIncomparable;

  @Inject
  TypeCompatibility(ErrorProneFlags flags) {
    this.treatBuildersAsIncomparable =
        flags.getBoolean("TypeCompatibility:TreatBuildersAsIncomparable").orElse(true);
  }

  public TypeCompatibilityReport compatibilityOfTypes(
      Type receiverType, Type argumentType, VisitorState state) {
    return compatibilityOfTypes(receiverType, argumentType, typeSet(state), typeSet(state), state);
  }

  private TypeCompatibilityReport compatibilityOfTypes(
      Type leftType,
      Type rightType,
      Set<Type> previouslySeenComponentsOfLeftType,
      Set<Type> previouslySeenComponentsOfRightType,
      VisitorState state) {

    if (leftType == null
        || rightType == null
        || leftType.getKind() == TypeKind.NULL
        || rightType.getKind() == TypeKind.NULL) {
      return TypeCompatibilityReport.compatible();
    }

    Types types = state.getTypes();
    Type leftUpperBound = getUpperBound(leftType, types);
    Type rightUpperBound = getUpperBound(rightType, types);
    if (types.isSameType(leftUpperBound, rightUpperBound)) {
      // As a special case, consider Builder classes without an equals implementation to not be
      // comparable to themselves. It would be reasonable to mistake Builders as having value
      // semantics, which may be misleading.
      if (treatBuildersAsIncomparable
          && !leftUpperBound.tsym.isEnum()
          && leftUpperBound.isFinal()
          && leftUpperBound.tsym.name.toString().endsWith("Builder")) {
        Names names = state.getNames();
        MethodSymbol equals =
            (MethodSymbol)
                state.getSymtab().objectType.tsym.members().findFirst(state.getNames().equals);
        return isEmpty(
                types
                    .membersClosure(leftUpperBound, /* skipInterface= */ false)
                    .getSymbolsByName(
                        names.toString,
                        m ->
                            m != equals
                                && m.overrides(
                                    equals, leftUpperBound.tsym, types, /* checkResult= */ false)))
            ? TypeCompatibilityReport.incompatible(leftType, rightType, WITHOUT_EQUALS_REASON)
            : TypeCompatibilityReport.compatible();
      }
      return TypeCompatibilityReport.compatible();
    }

    if (leftType.isPrimitive()
        && rightType.isPrimitive()
        && !isSameType(leftType, rightType, state)) {
      return TypeCompatibilityReport.incompatible(leftType, rightType);
    }

    if (!isFeasiblyCompatible(leftType, rightType, state)) {
      return TypeCompatibilityReport.incompatible(leftType, rightType);
    }

    // Now, see if we can find a generic superclass between the two types, and if so, check the
    // generic parameters for cast-compatibility:

    // class Super<T> (with an equals() override)
    // class Bar extends Super<String>
    // class Foo extends Super<Integer>
    // Bar and Foo would least-upper-bound to Super, and we compare String and Integer to each-other
    Type erasedLeftType = types.erasure(leftType);
    Type erasedRightType = types.erasure(rightType);
    Type commonSupertype = types.lub(erasedRightType, erasedLeftType);
    // primitives, etc. can't have a common superclass.
    if (commonSupertype.getTag().equals(TypeTag.BOT)
        || commonSupertype.getTag().equals(TypeTag.ERROR)) {
      return TypeCompatibilityReport.compatible();
    }

    // Detect a potential generics mismatch - if there are no generics, this will return a
    // compatible report.
    TypeCompatibilityReport compatibilityReport =
        checkForGenericsMismatch(
            leftType,
            rightType,
            commonSupertype,
            previouslySeenComponentsOfLeftType,
            previouslySeenComponentsOfRightType,
            state);

    if (!compatibilityReport.isCompatible()) {
      return compatibilityReport;
    }

    // At this point, we're pretty sure these types are compatible with each other. However, there
    // are certain scenarios where the normal processing believes that these two types are
    // compatible, but due to the way that certain classes' equals methods are constructed, they
    // deceive the normal processing into thinking they're compatible, but they are not.
    return areTypesIncompatibleCollections(leftType, rightType, commonSupertype, state)
            || areIncompatibleProtoTypes(erasedLeftType, erasedRightType, commonSupertype, state)
        ? TypeCompatibilityReport.incompatible(leftType, rightType)
        : TypeCompatibilityReport.compatible();
  }

  private static boolean isFeasiblyCompatible(Type leftType, Type rightType, VisitorState state) {
    // If one type can be cast into the other, they are potentially equal to each other.
    // Note: we do this precisely in this order to allow primitive values to be checked pre-1.7:
    // 1.6: java.lang.Object can't be cast to primitives
    // 1.7: java.lang.Object can be cast to primitives (implicitly through the boxed primitive type)
    if (isCastable(rightType, leftType, state)) {
      return true;
    }

    // Otherwise, we collect all overrides of java.lang.Object.equals() that the left type has.
    // If one of those overrides is inherited by the right type, then they share a common supertype
    // that defines its own equals method.
    Types types = state.getTypes();
    Symbol rightClass = getUpperBound(rightType, state.getTypes()).tsym;
    return findMatchingMethods(
            EQUALS.get(state), m -> customEqualsMethod(m, state), leftType, types)
        .stream()
        .anyMatch(method -> rightClass.isSubClass(method.enclClass(), types));
  }

  /**
   * Returns if the method represents an override of equals(Object) that is not on `Object` or
   * `Enum`.
   *
   * <p>This would represent an equals method that could specify equality semantics aside from
   * object identity.
   */
  private static boolean customEqualsMethod(MethodSymbol methodSymbol, VisitorState state) {
    ClassSymbol owningClass = methodSymbol.enclClass();
    return !methodSymbol.isStatic()
        && ((methodSymbol.flags() & Flags.SYNTHETIC) == 0)
        && state.getTypes().isSameType(methodSymbol.getReturnType(), state.getSymtab().booleanType)
        && methodSymbol.getParameters().size() == 1
        && state
            .getTypes()
            .isSameType(
                getOnlyElement(methodSymbol.getParameters()).type, state.getSymtab().objectType)
        && !owningClass.equals(state.getSymtab().objectType.tsym)
        && !owningClass.equals(state.getSymtab().enumSym);
  }

  /**
   * Detects the situation where two distinct subtypes of Collection (e.g.: List or Set) are
   * compared.
   *
   * <p>Since they share a common interface with an equals() declaration ({@code Collection<T>}),
   * normal processing will consider them compatible with each other, but each subtype of Collection
   * overrides equals() in such a way as to be incompatible with each other.
   */
  private static boolean areTypesIncompatibleCollections(
      Type leftType, Type rightType, Type nearestCommonSupertype, VisitorState state) {
    // We want to disallow equality between these collection sub-interfaces, but *do* want to
    // allow compatibility between Collection and List.
    Type collectionType = JAVA_UTIL_COLLECTION.get(state);
    return isSameType(nearestCommonSupertype, collectionType, state)
        && !isSameType(leftType, collectionType, state)
        && !isSameType(rightType, collectionType, state);
  }

  private boolean areIncompatibleProtoTypes(
      Type leftType, Type rightType, Type nearestCommonSupertype, VisitorState state) {
    // See discussion in b/152428396 - Proto equality is defined as having the "same message type",
    // with the same corresponding field values. However - there are 3 flavors of Java Proto API
    // that could represent the same message (proto1, mutable proto2, and immutable proto2 [as well
    // as immutable proto3, but those look like proto2 classes without "hazzer" method]).
    //
    // Protos share a common super-interface that defines an equals() method, but since every proto
    // message shares that supertype, we shouldn't let that shared equals() definition override our
    // attempts to find mismatched equals() between "really" unrelated objects.
    //
    // We do our best here to identify circumstances where the programmer likely got protos that are
    // unrelated (requiring special treatment above and beyond the normal logic in
    // compatibilityOfTypes), but ignore cases where the protos are *actually* equal to each other
    // according to its definition.

    // proto1: io.ProtocolMessage < p.AbstractMutableMessage < p.MutableMessage < p.Message
    // proto2-mutable: p.GeneratedMutableMessage < p.AbstractMutableMessage < ... as proto1
    // proto2-immutable: p.GeneratedMessage < p.AbstractMessage < p.Message

    // DynamicMessage is comparable to all other proto types.
    Type dynamicMessage = COM_GOOGLE_PROTOBUF_DYNAMICMESSAGE.get(state);
    if (isSameType(leftType, dynamicMessage, state)
        || isSameType(rightType, dynamicMessage, state)) {
      return false;
    }

    Type protoBase = COM_GOOGLE_PROTOBUF_MESSAGE.get(state);
    if (isSameType(nearestCommonSupertype, protoBase, state)
        && !isSameType(leftType, protoBase, state)
        && !isSameType(rightType, protoBase, state)) {
      // In this situation, there's a mix of (immutable proto2, others) messages. Here, we want to
      // figure out if the other is a proto2-mutable representing the same message/type as the
      // immutable proto.

      // While proto2 can be compared to proto1, this is somewhat of a historical accident. We don't
      // want to endorse this, and want to ban it. Protos of the same version and same descriptor
      // should be comparable, hence proto2-immutable and proto2-mutable can be.

      // See b/152428396#comment10, but basically, we inspect the names of the classes to guess
      // whether or not those types are actually representing the same type.
      String leftClassPart = classNamePart(leftType);
      String rightClassPart = classNamePart(rightType);

      return !classesAreMutableAndImmutableOfSameType(leftClassPart, rightClassPart)
          && !classesAreMutableAndImmutableOfSameType(rightClassPart, leftClassPart);
    }

    // Otherwise, if these two types are *concrete* proto classes, but not the same message, then
    // consider them incompatible with each other.
    Type messageLite = COM_GOOGLE_PROTOBUF_MESSAGELITE.get(state);
    return isSubtype(nearestCommonSupertype, messageLite, state)
        && isConcrete(leftType, state.getTypes())
        && isConcrete(rightType, state.getTypes());
  }

  private static boolean isConcrete(Type type, Types types) {
    Type toEvaluate = getUpperBound(type, types);
    return (toEvaluate.tsym.flags() & (Flags.ABSTRACT | Flags.INTERFACE)) == 0;
  }

  private static boolean classesAreMutableAndImmutableOfSameType(String l, String r) {
    return l.startsWith("Mutable") && l.substring("Mutable".length()).equals(r);
  }

  private static String classNamePart(Type type) {
    String fullClassname = type.asElement().getQualifiedName().toString();
    String packageName = enclosingPackage(type.asElement()).fullname.toString();
    String prefix = fullClassname.substring(packageName.length());
    return prefix.startsWith(".") ? prefix.substring(1) : prefix;
  }

  /**
   * Given a {@code leftType} and {@code rightType} (of the shared supertype {@code superType}),
   * compare the generic types of those two types (as projected against the {@code superType})
   * against each other.
   *
   * <p>If there are no generic types, or the generic types are compatible with each other, returns
   * a {@link TypeCompatibilityReport#isCompatible} report. Otherwise, returns a compatibility
   * report showing that a specific generic type in the projection of {@code leftType} is
   * incompatible with the specific generic type in the projection of {@code rightType}.
   */
  private TypeCompatibilityReport checkForGenericsMismatch(
      Type leftType,
      Type rightType,
      Type superType,
      Set<Type> previousLeftTypes,
      Set<Type> previousRightTypes,
      VisitorState state) {
    List<Type> leftGenericTypes = typeArgsAsSuper(leftType, superType, state);
    List<Type> rightGenericTypes = typeArgsAsSuper(rightType, superType, state);

    return Streams.zip(leftGenericTypes.stream(), rightGenericTypes.stream(), TypePair::new)
        // If we encounter an f-bound, skip that index's type when comparing the compatibility of
        // types to avoid infinite recursion:
        // interface Super<A extends Super<A, B>, B>
        // class Foo extends Super<Foo, String>
        // class Bar extends Super<Bar, Integer>
        .filter(
            tp ->
                !(previousLeftTypes.contains(tp.left)
                    || isSameType(tp.left, leftType, state)
                    || previousRightTypes.contains(tp.right)
                    || isSameType(tp.right, rightType, state)))
        .map(
            types -> {
              Set<Type> nextLeftTypes = typeSet(state);
              nextLeftTypes.addAll(previousLeftTypes);
              nextLeftTypes.add(leftType);
              Set<Type> nextRightTypes = typeSet(state);
              nextRightTypes.addAll(previousRightTypes);
              nextRightTypes.add(rightType);
              return compatibilityOfTypes(
                  types.left, types.right, nextLeftTypes, nextRightTypes, state);
            })
        .filter(tcr -> !tcr.isCompatible())
        .findFirst()
        .orElse(TypeCompatibilityReport.compatible());
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
    private static final TypeCompatibilityReport COMPATIBLE =
        new AutoValue_TypeCompatibility_TypeCompatibilityReport(true, null, null, null);

    public abstract boolean isCompatible();

    public abstract @Nullable Type lhs();

    public abstract @Nullable Type rhs();

    public abstract @Nullable String extraReason();

    static TypeCompatibilityReport compatible() {
      return COMPATIBLE;
    }

    static TypeCompatibilityReport incompatible(Type lhs, Type rhs) {
      return incompatible(lhs, rhs, "");
    }

    static TypeCompatibilityReport incompatible(Type lhs, Type rhs, String extraReason) {
      return new AutoValue_TypeCompatibility_TypeCompatibilityReport(false, lhs, rhs, extraReason);
    }
  }

  private static final class TypePair {
    final Type left;
    final Type right;

    TypePair(Type left, Type right) {
      this.left = left;
      this.right = right;
    }
  }

  private static final Supplier<Name> EQUALS =
      VisitorState.memoize(state -> state.getName("equals"));

  private static final Supplier<Type> COM_GOOGLE_PROTOBUF_DYNAMICMESSAGE =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.protobuf.DynamicMessage"));

  private static final Supplier<Type> COM_GOOGLE_PROTOBUF_MESSAGE =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.protobuf.Message"));

  private static final Supplier<Type> COM_GOOGLE_PROTOBUF_MESSAGELITE =
      VisitorState.memoize(state -> state.getTypeFromString("com.google.protobuf.MessageLite"));

  private static final Supplier<Type> JAVA_UTIL_COLLECTION =
      VisitorState.memoize(state -> state.getTypeFromString("java.util.Collection"));
}
