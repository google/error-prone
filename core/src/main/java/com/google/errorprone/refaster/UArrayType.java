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
import com.sun.tools.javac.code.Type.ArrayType;
import javax.annotation.Nullable;

/**
 * {@link UType} version of {@link ArrayType}, which represents a type {@code T[]} based on the type
 * {@code T}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UArrayType extends UType {
  public static UArrayType create(UType componentType) {
    return new AutoValue_UArrayType(componentType);
  }

  abstract UType componentType();

  @Override
  @Nullable
  public Choice<Unifier> visitArrayType(ArrayType arrayType, @Nullable Unifier unifier) {
    return componentType().unify(arrayType.getComponentType(), unifier);
  }

  @Override
  public ArrayType inline(Inliner inliner) throws CouldNotResolveImportException {
    return new ArrayType(componentType().inline(inliner), inliner.symtab().arrayClass);
  }
}
