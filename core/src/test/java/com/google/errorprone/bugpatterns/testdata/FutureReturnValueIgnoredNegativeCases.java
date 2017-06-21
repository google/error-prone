/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.util.concurrent.Futures.immediateFuture;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.channel.ChannelFuture;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;

/** */
public class FutureReturnValueIgnoredNegativeCases {

  public FutureReturnValueIgnoredNegativeCases() {}

  static ListenableFuture<Object> getFuture() {
    return immediateFuture(null);
  }

  interface CanIgnoreMethod {
    @CanIgnoreReturnValue
    Future<Object> getFuture();
  }

  public static class CanIgnoreImpl implements CanIgnoreMethod {
    @Override
    public Future<Object> getFuture() {
      return null;
    }
  }

  static void callIgnoredInterfaceMethod() {
    new CanIgnoreImpl().getFuture();
  }

  @CanIgnoreReturnValue
  static ListenableFuture<Object> getFutureIgnore() {
    return immediateFuture(null);
  }

  static void putInMap() {
    Map<Object, Future<?>> map = new HashMap<>();
    map.put(new Object(), immediateFuture(null));
    Map map2 = new HashMap();
    map2.put(new Object(), immediateFuture(null));
  }

  static void preconditions()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Preconditions.checkNotNull(getFuture());
    Preconditions.checkNotNull(new Object());
    FutureReturnValueIgnoredNegativeCases.class.getDeclaredMethod("preconditions").invoke(null);
  }

  static void checkIgnore() {
    getFutureIgnore();
  }

  void ignoreForkJoinTaskFork(ForkJoinTask<?> t) {
    t.fork();
  }

  void ignoreForkJoinTaskFork_subclass(RecursiveAction t) {
    t.fork();
  }

  void ignoreExecutorCompletionServiceSubmit(ExecutorCompletionService s) {
    s.submit(() -> null);
  }

  void ignoreChannelFutureAddListener(ChannelFuture cf) {
    cf.addListener((ChannelFuture f) -> {});
  }

  void ignoreChannelFutureAddListeners(ChannelFuture cf) {
    cf.addListeners((ChannelFuture f) -> {}, (ChannelFuture f) -> {});
  }
}
