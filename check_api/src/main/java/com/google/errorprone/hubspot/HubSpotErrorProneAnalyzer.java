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
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;

public class HubSpotErrorProneAnalyzer implements TaskListener {
  private final ErrorProneAnalyzer delegate;


  public static HubSpotErrorProneAnalyzer wrap(ErrorProneAnalyzer analyzer) {
    return new HubSpotErrorProneAnalyzer(analyzer);
  }

  private HubSpotErrorProneAnalyzer(ErrorProneAnalyzer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void started(TaskEvent e) {
    try {
      delegate.started(e);
    } catch (Throwable t) {
      HubSpotUtils.recordUncaughtException(t);
      throw t;
    }
  }

  @Override
  public void finished(TaskEvent e) {
    try {
      delegate.finished(e);
    } catch (Throwable t) {
      HubSpotUtils.recordUncaughtException(t);
      throw t;
    }
  }
}
