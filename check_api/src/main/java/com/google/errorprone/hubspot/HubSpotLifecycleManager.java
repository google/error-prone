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

package com.google.errorprone.hubspot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.ImmutableMap;
import com.sun.tools.javac.util.Context;

public class HubSpotLifecycleManager {
  private static final Context.Key<HubSpotLifecycleManager> timingsKey = new Context.Key<>();

  private final Set<Runnable> startupListener;
  private final Set<Runnable> shutdownListener;
  private final AtomicBoolean started;
  private final AtomicBoolean stopped;

  public static HubSpotLifecycleManager instance(Context context) {
    HubSpotLifecycleManager instance = context.get(timingsKey);
    if (instance == null) {
      instance = new HubSpotLifecycleManager(context);
    }
    return instance;
  }

  private HubSpotLifecycleManager(Context context) {
    this.startupListener = new HashSet<>();
    this.shutdownListener = new HashSet<>();
    this.started = new AtomicBoolean();
    this.stopped = new AtomicBoolean();

    context.put(timingsKey, this);
  }

  public void handleStartup() {
    if (started.compareAndSet(false, true)) {
      // order is important here
      writeCanary();
      startupListener.forEach(Runnable::run);
    } else {
      throw new RuntimeException("Startup called more than once!");
    }
  }

  public void handleShutdown() {
    if (!started.get()) {
      throw new RuntimeException("Shutdown called without startup!");
    } else if (stopped.compareAndSet(false, true)) {
      // order is important here
      shutdownListener.forEach(Runnable::run);
      deleteCanary();
    } else {
      throw new RuntimeException("Stopped called more than once!");
    }
  }

  public void addStartupListener(Runnable runnable) {
    startupListener.add(runnable);
  }

  public void addShutdownListener(Runnable runnable) {
    shutdownListener.add(runnable) ;
  }

  private void writeCanary() {
    getCanaryPath().ifPresent(p -> {
      try {
        JsonUtils.getMapper()
            .writeValue(
                p.toFile(),
                ImmutableMap.of("errorProneLifeCycleCanary", Objects.hash(this)));
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void deleteCanary() {
    getCanaryPath().ifPresent(p -> {
      try {
        Files.delete(p);
      } catch (Exception e) {
        throw new RuntimeException("Failed to delete canary!", e);
      }
    });
  }

  private Optional<Path> getCanaryPath() {
    return FileManager.getLifeCycleCanaryPath(String.valueOf(Objects.hash(this)));
  }
}
