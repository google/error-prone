/*
 * Copyright 2016 The Error Prone Authors.
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

import com.google.common.collect.Iterables;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;

/** Loads custom Error Prone checks from the annotation processor classpath. */
public class ErrorPronePlugins {

  public static ScannerSupplier loadPlugins(ScannerSupplier scannerSupplier, Context context) {
    JavaFileManager fileManager = context.get(JavaFileManager.class);
    // Unlike in annotation processor discovery, we never search CLASS_PATH if
    // ANNOTATION_PROCESSOR_PATH is unavailable.
    if (!fileManager.hasLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH)) {
      return scannerSupplier;
    }
    // Use the same classloader that Error Prone was loaded from to avoid classloader skew
    // when using Error Prone plugins together with the Error Prone javac plugin.
    JavacProcessingEnvironment processingEnvironment = JavacProcessingEnvironment.instance(context);
    List<ClassLoader> allClassLoaders = new ArrayList<>(2);
    allClassLoaders.add(processingEnvironment.getProcessorClassLoader());
    allClassLoaders.add(ErrorPronePlugins.class.getClassLoader());

    List<Iterable<BugChecker>> bugCheckersByClassLoader = new ArrayList<>(allClassLoaders.size());
    for (ClassLoader loader : allClassLoaders) {
      bugCheckersByClassLoader.add(ServiceLoader.load(BugChecker.class, loader));
    }
    Iterable<BugChecker> extraBugCheckers = Iterables.concat(bugCheckersByClassLoader);
    if (Iterables.isEmpty(extraBugCheckers)) {
      return scannerSupplier;
    }
    return scannerSupplier.plus(
        ScannerSupplier.fromBugCheckerClasses(
            Iterables.transform(extraBugCheckers, BugChecker::getClass)));
  }
}
