/*
 * Copyright 2016 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.android;

import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.isArrayType;
import static com.google.errorprone.matchers.Matchers.isPrimitiveType;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.typeCast;
import static com.google.errorprone.matchers.method.MethodMatchers.instanceMethod;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.getType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.TypeCastTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.code.Types;

/** @author epmjohnston@google.com (Emily P.M. Johnston) */
@BugPattern(
    name = "BundleDeserializationCast",
    summary = "Object serialized in Bundle may have been flattened to base type.",
    severity = ERROR)
public class BundleDeserializationCast extends BugChecker implements TypeCastTreeMatcher {

  private static final Matcher<TypeCastTree> BUNDLE_DESERIALIZATION_CAST_EXPRESSION =
      typeCast(
          anything(), instanceMethod().onExactClass("android.os.Bundle").named("getSerializable"));

  @Override
  public Description matchTypeCast(TypeCastTree tree, VisitorState state) {
    if (!state.isAndroidCompatible()) {
      return Description.NO_MATCH;
    }
    if (!BUNDLE_DESERIALIZATION_CAST_EXPRESSION.matches(tree, state)) {
      return NO_MATCH;
    }

    Tree targetType = tree.getType();

    // Casting to primitive types shouldn't cause issues since they extend no type and are final.
    if (isPrimitiveType().matches(targetType, state)) {
      return NO_MATCH;
    }

    /*
     * Ordering of these checks determines precedence of types (which type *should* be cast to).
     * Deduced by inspecting serialization code, see
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/os/Parcel.java#1377
     * Simply allow specially handled final types, and check that other specially handled types
     * are cast to their base types.
     *
     * There is no documentation of what types are safe to cast to, so this code is paralleling the
     * code cited above to emulate the same logic in order to produce the correct behavior.
     */
    if (isSameType("java.lang.String").matches(targetType, state)) {
      return NO_MATCH;
    }
    if (isSubtypeOf("java.util.Map").matches(targetType, state)) {
      // Make an exception for HashMap.
      return anyOf(isSameType("java.util.Map"), isSameType("java.util.HashMap"))
              .matches(targetType, state)
          ? NO_MATCH
          : getDescriptionForType(tree, "Map");
    }
    // All Parcelables handled after this point have their types preserved.
    if (isSubtypeOf("android.os.Parcelable").matches(targetType, state)) {
      return NO_MATCH;
    }
    if (isSubtypeOf("java.lang.CharSequence").matches(targetType, state)) {
      return isSameType("java.lang.CharSequence").matches(targetType, state)
          ? NO_MATCH
          : getDescriptionForType(tree, "CharSequence");
    }
    if (isSubtypeOf("java.util.List").matches(targetType, state)) {
      // Make an exception for ArrayList.
      return anyOf(isSameType("java.util.List"), isSameType("java.util.ArrayList"))
              .matches(targetType, state)
          ? NO_MATCH
          : getDescriptionForType(tree, "List");
    }
    if (isSubtypeOf("android.util.SparseArray").matches(targetType, state)) {
      return isSameType("android.util.SparseArray").matches(targetType, state)
          ? NO_MATCH
          : getDescriptionForType(tree, "SparseArray");
    }

    // Check component types of arrays. The only type that may cause problems is CharSequence[].
    if (isArrayType().matches(targetType, state)) {
      Type componentType = ((ArrayType) getType(targetType)).getComponentType();
      Types types = state.getTypes();
      Type charSequenceType = typeFromString("java.lang.CharSequence").get(state);
      Type stringType = typeFromString("java.lang.String").get(state);
      // Okay to cast to String[] because String[] is written before CharSequence[]
      // in the serialization code.
      if (types.isSubtype(componentType, charSequenceType)
          && !types.isSameType(componentType, charSequenceType)
          && !types.isSameType(componentType, stringType)) {
        return getDescriptionForType(tree, "CharSequence[]");
      }
    }
    return NO_MATCH;
  }

  private Description getDescriptionForType(TypeCastTree tree, String baseType) {
    String targetType = getType(tree.getType()).toString();
    return buildDescription(tree)
        .setMessage(
            String.format(
                "When serialized in Bundle, %s may be transformed into an arbitrary subclass of %s."
                    + " Please cast to %s.",
                targetType, baseType, baseType))
        .build();
  }
}
