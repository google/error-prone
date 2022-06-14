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

import com.google.errorprone.ErrorProneAnalyzer;
import com.google.errorprone.ErrorProneOptions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.util.Context;

public class HubSpotErrorProneAnalyzer implements TaskListener {
  private final Context context;
  private final ErrorProneOptions options;
  private final ErrorProneAnalyzer delegate;


  public static TaskListener wrap(Context context, ErrorProneOptions options, ErrorProneAnalyzer analyzer) {
    return new HubSpotErrorProneAnalyzer(context, options, analyzer);
  }

  private HubSpotErrorProneAnalyzer(Context context, ErrorProneOptions options, ErrorProneAnalyzer delegate) {
    this.context = context;
    this.options = options;
    this.delegate = delegate;
  }

  @Override
  public void started(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleStartup();
    }

    try {
      delegate.started(taskEvent);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(options)) {
        HubSpotUtils.recordUncaughtException(t);
      }

      throw t;
    }
  }

  @Override
  public void finished(TaskEvent taskEvent) {
    if (taskEvent.getKind() == Kind.COMPILATION) {
      HubSpotLifecycleManager.instance(context).handleShutdown();
    }

    try {
      delegate.finished(taskEvent);
    } catch (Throwable t) {
      if (HubSpotUtils.isErrorHandlingEnabled(options)) {
        HubSpotUtils.recordUncaughtException(t);
      }

      throw t;
    }
  }
}
