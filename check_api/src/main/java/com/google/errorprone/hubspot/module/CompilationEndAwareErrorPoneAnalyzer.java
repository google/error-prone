/*
 * Copyright 2022 The Error Prone Authors.
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

package com.google.errorprone.hubspot.module;

import java.util.List;
import java.util.Set;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.reflect.Reflection;
import com.google.errorprone.ErrorProneAnalyzer;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.ErrorPronePlugins;
import com.google.errorprone.InvalidCommandLineOptionException;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.descriptionlistener.DescriptionListeners;
import com.google.errorprone.hubspot.HubSpotUtils;
import com.google.errorprone.scanner.ErrorProneScanner;
import com.google.errorprone.scanner.ErrorProneScannerTransformer;
import com.google.errorprone.scanner.Scanner;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.PropagatedException;

public class CompilationEndAwareErrorPoneAnalyzer implements TaskListener {


  /**
   * @see ErrorProneAnalyzer#createByScanningForPlugins(ScannerSupplier, ErrorProneOptions, Context)
   * @return
   */
  public static CompilationEndAwareErrorPoneAnalyzer create(ScannerSupplier scannerSupplier, ErrorProneOptions errorProneOptions, Context context) {
    context.put(ErrorProneFlags.class, errorProneOptions.getFlags());

    Supplier<Scanner> memoizedScanner = Suppliers.memoize(
        () -> {
          // we can't load plugins from the processorpath until the filemanager has been
          // initialized, so do it lazily
          try {
                return ErrorPronePlugins.loadPlugins(scannerSupplier, errorProneOptions, context)
                    .applyOverrides(errorProneOptions)
                    .get();
          } catch (InvalidCommandLineOptionException e) {
            throw new PropagatedException(e);
          }
        });

    ErrorProneAnalyzer delegate = new ErrorProneAnalyzer(
        Suppliers.memoize(() -> ErrorProneScannerTransformer.create(memoizedScanner.get())),
        errorProneOptions,
        context,
        DescriptionListeners.factory(context)
    );

    return new CompilationEndAwareErrorPoneAnalyzer(memoizedScanner, errorProneOptions, context, delegate);
  }

  // This class takes a memoized scanner so that we can later access the same instances
  // of BugChecker that were used by the actual tree traversal. A bit of a hack, but error-pone
  // doesn't really provide a better way to get at the checks
  private final Supplier<Scanner> memoizedScanner;
  private final ErrorProneOptions errorProneOptions;
  private final Context context;
  private final ErrorProneAnalyzer delegate;

  private boolean hasHadFatalError = false;

  private CompilationEndAwareErrorPoneAnalyzer(Supplier<Scanner> memoizedScanner,
                                               ErrorProneOptions errorProneOptions,
                                               Context context,
                                               ErrorProneAnalyzer delegate) {
    this.memoizedScanner = memoizedScanner;
    this.errorProneOptions = errorProneOptions;
    this.context = context;
    this.delegate = delegate;
  }

  @Override
  public void started(TaskEvent e) {
    try {
      delegate.started(e);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(errorProneOptions)) {
        HubSpotUtils.recordUncaughtException(t);
      }

      hasHadFatalError = true;

      throw t;
    }
  }

  @Override
  public void finished(TaskEvent e) {
    try {
      delegate.finished(e);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(errorProneOptions)) {
        HubSpotUtils.recordUncaughtException(t);
      }

      hasHadFatalError = true;

      throw t;
    }

    if (e.getKind() == Kind.COMPILATION && !hasHadFatalError) {
      onModuleFinished();
    }
  }

  public void onModuleFinished() {
    try {
      Scanner scanner = memoizedScanner.get();
      if (!(scanner instanceof ErrorProneScanner)) {

        if (scanner.getClass().getEnclosingClass() != null && Reflection.getPackageName(scanner.getClass()).startsWith("com.google.errorprone.")) {
          // This happens in some tests that still use deprecated testing methods
          return;
        } else {
          throw new IllegalStateException("Unexpected scanner type " + scanner.getClass());
        }
      }

      ModuleState moduleState = ModuleState.create(
          context,
          DescriptionListeners.factory(context),
          scanner.severityMap(),
          errorProneOptions
      );

      for (BugChecker bugChecker : ((ErrorProneScanner)scanner).getBugCheckers()) {
        if (bugChecker instanceof ModuleFinishedMatcher) {
          moduleState.reportMatch(
              ((ModuleFinishedMatcher)bugChecker).visitFinishedModule(moduleState)
          );
        }
      }
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(errorProneOptions)) {
        HubSpotUtils.recordUncaughtException(t);
      }
      throw t;
    }
  }
}
