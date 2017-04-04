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
import com.google.common.collect.Lists;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.tools.javac.util.Context;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
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

    List<Class<? extends BugChecker>> bugCheckerClasses;
    // Synchronize to prevent races
    synchronized(ErrorPronePlugins.class) {
      // disable URL caching to avoid stale state bugs when run within a daemon-building process
      boolean oldURLCaching = setUrlCaching(false);
      Iterable<BugChecker> extraBugCheckers = ServiceLoader.load(BugChecker.class, loader);
      bugCheckerClasses = Lists.newArrayList(Iterables.transform(extraBugCheckers, GET_CLASS));
      // restore the old URL caching setting
      setUrlCaching(oldURLCaching);
    }
    if (bugCheckerClasses.isEmpty()) {
      return scannerSupplier;
    } else {
      return scannerSupplier.plus(ScannerSupplier.fromBugCheckerClasses(bugCheckerClasses));
    }
  }

  /**
   * A dummy {@link URLConnection} object used for disabling the global setting to use caches
   */
  private static URLConnection DUMMY_CONNECTION = null;

  static {
    try {
      DUMMY_CONNECTION = new URLConnection(new URL("file:///")) {
        @Override
        public void connect() throws IOException {

        }
      };
    } catch (MalformedURLException e) {
      // this should never fail
      throw new RuntimeException(e);
    }
  }

  /**
   * Update the global caching setting for {@link URLConnection}.  We need this hackish implementation since
   * for some reason, the global caching setting can only be controlled via an instance method.
   */
  private static boolean setUrlCaching(boolean enable) {
    boolean prevEnabled = DUMMY_CONNECTION.getDefaultUseCaches();
    DUMMY_CONNECTION.setDefaultUseCaches(enable);
    return prevEnabled;
  }
}
