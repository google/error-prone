/*
 * Copyright 2013 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.refaster;

import static com.google.errorprone.refaster.Unifier.unifications;
import static com.sun.tools.javac.code.Flags.STATIC;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ClassType;
import java.util.List;

/**
 * A representation of a type with optional generic parameters.
 *
 * @author Louis Wasserman
 */
@AutoValue
public abstract class UClassType extends UType {
  public static UClassType create(CharSequence fullyQualifiedClass, List<UType> typeArguments) {
    return new AutoValue_UClassType(
        StringName.of(fullyQualifiedClass), ImmutableList.copyOf(typeArguments));
  }

  public static UClassType create(String fullyQualifiedClass, UType... typeArguments) {
    return create(fullyQualifiedClass, ImmutableList.copyOf(typeArguments));
  }

  abstract StringName fullyQualifiedClass();

  abstract List<UType> typeArguments();

  @Override
  public Choice<Unifier> visitClassType(ClassType classType, Unifier unifier) {
    return fullyQualifiedClass()
        .unify(classType.tsym.getQualifiedName(), unifier)
        .thenChoose(unifications(typeArguments(), classType.getTypeArguments()));
  }

  @Override
  public ClassType inline(Inliner inliner) throws CouldNotResolveImportException {
    ClassSymbol classSymbol = inliner.resolveClass(fullyQualifiedClass());
    boolean isNonStaticInnerClass =
        classSymbol.owner instanceof ClassSymbol && (classSymbol.flags() & STATIC) == 0;
    Type owner = isNonStaticInnerClass ? classSymbol.owner.type : Type.noType;
    return new ClassType(owner, inliner.<Type>inlineList(typeArguments()), classSymbol);
  }
}
