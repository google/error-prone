/*
 * Copyright 2014 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.threadsafety;

import java.util.Collection;
import javax.annotation.CheckReturnValue;
import org.pcollections.Empty;
import org.pcollections.PSet;

/**
 * A set of held locks.
 *
 * <p>Wrapper around a {@link PSet} of {@link GuardedByExpression}s. Using a persistent collection
 * makes it easy to handle adding locks to the set only while visiting the scope where those locks
 * are held, without mutating the underlying collection.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
class HeldLockSet {

  final PSet<GuardedByExpression> locks;

  private HeldLockSet() {
    this(Empty.<GuardedByExpression>set());
  }

  private HeldLockSet(PSet<GuardedByExpression> locks) {
    this.locks = locks;
  }

  static HeldLockSet empty() {
    return new HeldLockSet();
  }

  @CheckReturnValue
  public HeldLockSet plus(GuardedByExpression lock) {
    return new HeldLockSet(locks.plus(lock));
  }

  @CheckReturnValue
  public HeldLockSet plusAll(Collection<GuardedByExpression> locks) {
    return new HeldLockSet(this.locks.plusAll(locks));
  }

  public Collection<GuardedByExpression> allLocks() {
    return locks;
  }

  @Override
  public String toString() {
    return locks.toString();
  }
}
