/*
 * Copyright 2021 The Error Prone Authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.tools.JavaFileObject;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.file.PathFileObject;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.util.Context;

public class HubSpotLifecycleManager {
  private static final Context.Key<HubSpotLifecycleManager> timingsKey = new Context.Key<>();

  private final Set<String> filesToAnalyze;
  private final Set<Runnable> listeners;
  private final Context context;

  public static HubSpotLifecycleManager instance(Context context) {
    HubSpotLifecycleManager instance = context.get(timingsKey);
    if (instance == null) {
      instance = new HubSpotLifecycleManager(context);
    }
    return instance;
  }

  private HubSpotLifecycleManager(Context context) {
    this.filesToAnalyze = new HashSet<>();
    this.listeners = new HashSet<>();
    this.context = context;

    Arguments.instance(context)
        .getFileObjects()
        .stream()
        .map(this::getFileKey)
        .forEach(filesToAnalyze::add);

    context.put(timingsKey, this);
  }

  public void markComplete(TaskEvent event) {
    boolean removed = filesToAnalyze.remove(getFileKey(event.getSourceFile()));
    if (!removed) {
      throw new RuntimeException("Saw unexpected file to remove: " + event.getSourceFile().getName());
    }

    if (filesToAnalyze.isEmpty()) {
      listeners.forEach(Runnable::run);
    }
  }

  public void addCompletionRunnable(Runnable runnable) {
    listeners.add(runnable);
  }

  private String getFileKey(JavaFileObject fileObject) {
    return fileObject.toUri().toString();
  }
}
