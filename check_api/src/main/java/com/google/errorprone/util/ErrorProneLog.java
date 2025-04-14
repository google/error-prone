/*
 * Copyright 2025 The Error Prone Authors.
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

package com.google.errorprone.util;

import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Log.DeferredDiagnosticHandler;
import com.sun.tools.javac.util.Log.DiscardDiagnosticHandler;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Compatibility shims for {@link Log}.
 *
 * <ul>
 *   <li>Return type and static inner classes changed in JDK 25
 *       https://github.com/openjdk/jdk/commit/4890b74c048a1472b87687294c316ecfb324e4ba
 * </ul>
 */
public final class ErrorProneLog {

  private static final Constructor<DiscardDiagnosticHandler>
      DISCARD_DIAGNOSTIC_HANDLER_CONSTRUCTOR = getDiscardDiagnosticHandlerConstructor();

  private static Constructor<DiscardDiagnosticHandler> getDiscardDiagnosticHandlerConstructor() {
    try {
      return DiscardDiagnosticHandler.class.getConstructor(Log.class);
    } catch (NoSuchMethodException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  public static Log.DiagnosticHandler discardDiagnosticHandler(Log log) {
    try {
      return DISCARD_DIAGNOSTIC_HANDLER_CONSTRUCTOR.newInstance(log);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private static final Constructor<DeferredDiagnosticHandler>
      DEFERRED_DIAGNOSTIC_HANDLER_CONSTRUCTOR = getDeferredDiagnosticHandlerConstructor();

  private static Constructor<DeferredDiagnosticHandler> getDeferredDiagnosticHandlerConstructor() {
    try {
      return DeferredDiagnosticHandler.class.getConstructor(Log.class);
    } catch (NoSuchMethodException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  public static DeferredDiagnosticHandler deferredDiagnosticHandler(Log log) {
    try {
      return DEFERRED_DIAGNOSTIC_HANDLER_CONSTRUCTOR.newInstance(log);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private static final Method GET_DIAGNOSTICS = getGetDiagnosticsMethod();

  private static Method getGetDiagnosticsMethod() {
    try {
      return DeferredDiagnosticHandler.class.getMethod("getDiagnostics");
    } catch (NoSuchMethodException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  @SuppressWarnings("unchecked")
  public static Collection<JCDiagnostic> getDiagnostics(DeferredDiagnosticHandler diagnostics) {
    try {
      return (Collection<JCDiagnostic>) GET_DIAGNOSTICS.invoke(diagnostics);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private ErrorProneLog() {}
}
