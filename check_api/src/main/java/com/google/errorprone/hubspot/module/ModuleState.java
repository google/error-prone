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

import com.google.common.base.Preconditions;
import com.google.errorprone.matchers.Description;
import java.util.Map;

import com.google.errorprone.BugPattern.SeverityLevel;
import com.google.errorprone.DescriptionListener;
import com.google.errorprone.ErrorProneOptions;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

public class ModuleState {
  public static ModuleState create(
      Context context,
      DescriptionListener.Factory listenerFactory,
      Map<String, SeverityLevel> severityMap,
      ErrorProneOptions errorProneOptions) {
    return new ModuleState(context, listenerFactory, severityMap, errorProneOptions);
  }

  private final Context context;
  private final DescriptionListener.Factory listenerFactory;
  private final Map<String, SeverityLevel> severityMap;
  private final ErrorProneOptions errorProneOptions;

  private ModuleState(Context context, DescriptionListener.Factory listenerFactory, Map<String, SeverityLevel> severityMap, ErrorProneOptions errorProneOptions) {

    this.context = context;
    this.listenerFactory = listenerFactory;
    this.severityMap = severityMap;
    this.errorProneOptions = errorProneOptions;
  }

  public Context getContext() {
    return context;
  }

  public Map<String, SeverityLevel> getSeverityMap() {
    return severityMap;
  }

  public ErrorProneOptions getErrorProneOptions() {
    return errorProneOptions;
  }

  public void reportMatch(ModuleDescription moduleDescription) {
    Preconditions.checkNotNull(moduleDescription, "Use ModuleDescription.NO_MATCH to denote an absent finding.");
    if (moduleDescription == ModuleDescription.NO_MATCH) {
      return;
    }

    SeverityLevel override = getSeverityMap().get(moduleDescription.getCheckName());
    if (override != null) {
      moduleDescription = moduleDescription.applySeverityOverride(override);
    }

    listenerFactory
        .getDescriptionListener(Log.instance(context), moduleDescription.getCompilationUnit())
        .onDescribed(moduleDescription.getDescription());
  }

  // Add more methods from visitorState as needed
}
