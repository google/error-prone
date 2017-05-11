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

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.ForAll;
import com.sun.tools.javac.code.Types;
import java.util.List;

/**
 * {@link UType} version of {@link ForAll}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UForAll extends UType {
  public static UForAll create(List<UTypeVar> typeVars, UType quantifiedType) {
    return new AutoValue_UForAll(ImmutableList.copyOf(typeVars), quantifiedType);
  }

  public abstract List<UTypeVar> getTypeVars();

  public abstract UType getQuantifiedType();

  @Override
  public Choice<Unifier> visitForAll(ForAll target, Unifier unifier) {
    Types types = unifier.types();
    try {
      Type myType = inline(new Inliner(unifier.getContext(), Bindings.create()));
      return Choice.condition(
          types.overrideEquivalent(types.erasure(myType), types.erasure(target)), unifier);
    } catch (CouldNotResolveImportException e) {
      return Choice.none();
    }
  }

  @Override
  public Type inline(Inliner inliner) throws CouldNotResolveImportException {
    return new ForAll(inliner.<Type>inlineList(getTypeVars()), getQuantifiedType().inline(inliner));
  }
}
