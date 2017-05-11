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

import com.google.auto.value.AutoValue;
import com.sun.tools.javac.code.BoundKind;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;

/**
 * {@link UType} version of {@link WildcardType}.
 *
 * @author lowasser@google.com (Louis Wasserman)
 */
@AutoValue
abstract class UWildcardType extends UType {
  public static UWildcardType create(BoundKind boundKind, UType bound) {
    return new AutoValue_UWildcardType(boundKind, bound);
  }

  /** This corresponds to a plain ? wildcard. */
  public static UWildcardType create() {
    return create(BoundKind.UNBOUND, UClassType.create("java.lang.Object"));
  }

  abstract BoundKind boundKind();

  abstract UType bound();

  @Override
  public Choice<Unifier> visitWildcardType(WildcardType wildcard, Unifier unifier) {
    return Choice.condition(boundKind().equals(wildcard.kind), unifier)
        .thenChoose(unifications(bound(), wildcard.type));
  }

  @Override
  public Type inline(Inliner inliner) throws CouldNotResolveImportException {
    return new WildcardType(bound().inline(inliner), boundKind(), inliner.symtab().boundClass);
  }
}
