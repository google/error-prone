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

package com.google.errorprone.scanner;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.MaturityLevel;
import com.google.errorprone.bugpatterns.BugChecker;

import java.io.IOException;

/**
 * Static helper class that provides {@link ScannerSupplier}s and {@link BugChecker}s
 * for the built-in error-prone checks, as opposed to plugin checks or checks used in tests.
 */
public class BuiltInCheckerSuppliers {

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in error-prone.
   */
  public static ScannerSupplier allChecks() {
    return ScannerSupplier.fromBugCheckerClasses(BUILT_IN_CHECKERS_LIST);
  }

  /**
   * Returns a {@link ScannerSupplier} with all {@link BugChecker}s in error-prone that have
   * {@code maturity == MaturityLevel.MATURE}.
   */
  public static ScannerSupplier matureChecks() {
    return allChecks().filter(MATURE);
  }

  /**
   * A list of all {@link BugChecker} classes known reflectively to error-prone.
   *
   * <p>TODO(user): This may be slow if the compiler classpath (not the compilation classpath) is
   * large.  Consider using an annotation processor to compute this list at compile time to avoid
   * reflection.
   */
  private static final ImmutableList<Class<? extends BugChecker>> BUILT_IN_CHECKERS_LIST;
  static {
    ImmutableList.Builder<Class<? extends BugChecker>> listBuilder = ImmutableList.builder();
    ClassPath classPath;
    try {
      classPath = ClassPath.from(ScannerSupplier.class.getClassLoader());
    } catch (IOException e) {
      throw new LinkageError();
    }
    for (ClassInfo classInfo : classPath.getAllClasses()) {
      // We could allow classes in other packages to be auto-discovered, but loading everything
      // on the classpath is slower and requires more error handling.
      if (!classInfo.getPackageName().startsWith("com.google.errorprone.bugpatterns")) {
        continue;
      }
      Class<?> clazz = classInfo.load();
      if (clazz.isAnnotationPresent(BugPattern.class) && BugChecker.class.isAssignableFrom(clazz)) {
        listBuilder.add(clazz.asSubclass(BugChecker.class));
      }
    }
    BUILT_IN_CHECKERS_LIST = listBuilder.build();
  }

  /**
   * A predicate for mature checks.
   */
  private static final Predicate<BugChecker> MATURE = new Predicate<BugChecker>() {
    @Override
    public boolean apply(BugChecker input) {
      return (input.maturity() == MaturityLevel.MATURE);
    }
  };

  // May not be instantiated
  private BuiltInCheckerSuppliers() {}
}
