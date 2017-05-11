/*
 * Copyright 2014 Google Inc. All rights reserved.
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

import static com.google.errorprone.refaster.Unifier.unifyList;
import static com.sun.tools.javac.code.Flags.COMPOUND;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type.ClassType;
import com.sun.tools.javac.code.Type.IntersectionClassType;

/**
 * {@code UType} representation of an {@code IntersectionClassType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UIntersectionClassType extends UType {
  static UIntersectionClassType create(Iterable<? extends UType> bounds) {
    return new AutoValue_UIntersectionClassType(ImmutableList.copyOf(bounds));
  }

  abstract ImmutableList<UType> bounds();

  @Override
  public IntersectionClassType inline(Inliner inliner) throws CouldNotResolveImportException {
    return new IntersectionClassType(
        inliner.inlineList(bounds()),
        new ClassSymbol(COMPOUND, inliner.asName("intersection"), inliner.symtab().noSymbol),
        false);
  }

  @Override
  public Choice<Unifier> visitClassType(ClassType t, Unifier unifier) {
    if (t instanceof IntersectionClassType) {
      IntersectionClassType intersection = (IntersectionClassType) t;
      return unifyList(unifier, bounds(), intersection.getComponents());
    }
    return Choice.none();
  }
}
