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

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Types;
import javax.annotation.Nullable;

/**
 * A serializable representation of a type template, used for enforcing type constraints on target
 * ASTs.
 *
 * @author Louis Wasserman
 */
public abstract class UType extends Types.SimpleVisitor<Choice<Unifier>, Unifier>
    implements Unifiable<Type>, Inlineable<Type> {

  @Override
  @Nullable
  public Choice<Unifier> visitType(Type t, @Nullable Unifier unifier) {
    return Choice.none();
  }

  @Override
  public final Choice<Unifier> unify(Type target, Unifier unifier) {
    return (target != null) ? target.accept(this, unifier) : Choice.<Unifier>none();
  }
}
