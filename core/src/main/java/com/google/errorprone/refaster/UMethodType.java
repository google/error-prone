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

import static com.google.errorprone.refaster.Unifier.unifyList;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.MethodType;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A {@code UType} representation of a {@link MethodType}. This can be used to e.g. disambiguate
 * method overloads.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
public abstract class UMethodType extends UType {

  public static UMethodType create(UType returnType, UType... parameterTypes) {
    return create(returnType, ImmutableList.copyOf(parameterTypes));
  }

  public static UMethodType create(UType returnType, List<UType> parameterTypes) {
    return new AutoValue_UMethodType(returnType, ImmutableList.copyOf(parameterTypes));
  }

  public abstract UType getReturnType();

  public abstract List<UType> getParameterTypes();

  @Override
  @Nullable
  public Choice<Unifier> visitMethodType(MethodType methodTy, @Nullable Unifier unifier) {
    // Don't unify the return type, which doesn't matter in overload resolution.
    return unifyList(unifier, getParameterTypes(), methodTy.getParameterTypes());
  }

  @Override
  public MethodType inline(Inliner inliner) throws CouldNotResolveImportException {
    return new MethodType(
        inliner.<Type>inlineList(getParameterTypes()),
        getReturnType().inline(inliner),
        com.sun.tools.javac.util.List.<Type>nil(),
        inliner.symtab().methodClass);
  }
}
