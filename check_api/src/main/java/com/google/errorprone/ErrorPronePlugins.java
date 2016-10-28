/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.util.Context;
import java.util.ServiceLoader;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/** Loads custom Error Prone checks from the annotation processor classpath. */
public class ErrorPronePlugins {

  private static final Function<BugChecker, Class<? extends BugChecker>> GET_CLASS =
      new Function<BugChecker, Class<? extends BugChecker>>() {
        @Override
        public Class<? extends BugChecker> apply(BugChecker input) {
          return input.getClass();
        }
      };

  public static ScannerSupplier loadPlugins(ScannerSupplier scannerSupplier, Context context) {

    JavaFileManager fileManager = context.get(JavaFileManager.class);
    // Search ANNOTATION_PROCESSOR_PATH if it's available. Unlike in annotation processor
    // discovery, we never search CLASS_PATH.
    if (!fileManager.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH)) {
      return scannerSupplier;
    }
    ClassLoader loader = fileManager.getClassLoader(StandardLocation.ANNOTATION_PROCESSOR_PATH);
    Iterable<BugChecker> extraBugCheckers = ServiceLoader.load(BugChecker.class, loader);
    if (Iterables.isEmpty(extraBugCheckers)) {
      return scannerSupplier;
    }
    return scannerSupplier.plus(
        ScannerSupplier.fromBugCheckerClasses(Iterables.transform(extraBugCheckers, GET_CLASS)));
  }
}
