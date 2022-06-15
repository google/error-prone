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

package com.google.errorprone.hubspot;

import com.google.errorprone.ErrorProneOptions;
import com.google.errorprone.hubspot.module.CompilationEndAwareErrorPoneAnalyzer;
import com.google.errorprone.scanner.ScannerSupplier;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.ClientCodeWrapper.Trusted;
import com.sun.tools.javac.util.Context;

@Trusted
public class HubSpotErrorProneAnalyzer implements TaskListener {
  private final Context context;
  private final ErrorProneOptions options;

  private final CompilationEndAwareErrorPoneAnalyzer compilationEndAwareErrorPoneAnalyzer;


  public static HubSpotErrorProneAnalyzer create(ScannerSupplier scannerSupplier, ErrorProneOptions options, Context context) {
    // Note: This analyzer doesn't make any attempt to handle refaster refactorings. We fall back on
    // standard analyzer impl if refaster is requested
    CompilationEndAwareErrorPoneAnalyzer compilationEndAwareErrorPoneAnalyzer = CompilationEndAwareErrorPoneAnalyzer
        .create(scannerSupplier, options, context);

    return new HubSpotErrorProneAnalyzer(context, options, compilationEndAwareErrorPoneAnalyzer);
  }

  private HubSpotErrorProneAnalyzer(
      Context context,
      ErrorProneOptions options,
      CompilationEndAwareErrorPoneAnalyzer compilationEndAwareErrorPoneAnalyzer
  ) {
    this.context = context;
    this.options = options;
    this.compilationEndAwareErrorPoneAnalyzer = compilationEndAwareErrorPoneAnalyzer;
  }

  @Override
  public void started(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleStartup();
    }

    compilationEndAwareErrorPoneAnalyzer.started(taskEvent);
  }

  @Override
  public void finished(TaskEvent taskEvent) {
    compilationEndAwareErrorPoneAnalyzer.finished(taskEvent);

    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleShutdown();
    }
  }
}
