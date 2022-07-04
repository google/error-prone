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

package com.google.errorprone.util;

import static com.google.common.base.Preconditions.checkState;

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.LookupKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A compatibility wrapper around {@code com.sun.tools.javac.util.Filter} */
public final class ErrorProneScope {

  @SuppressWarnings("unchecked") // reflection
  public Iterable<Symbol> getSymbolsByName(Name name, Predicate<Symbol> predicate) {
    return (Iterable<Symbol>) invoke(getSymbolsByName, name, maybeAsFilter(predicate));
  }

  @SuppressWarnings("unchecked") // reflection
  public Iterable<Symbol> getSymbolsByName(
      Name name, Predicate<Symbol> predicate, LookupKind lookupKind) {
    return (Iterable<Symbol>)
        invoke(getSymbolsByNameLookupKind, name, maybeAsFilter(predicate), lookupKind);
  }

  @SuppressWarnings("unchecked") // reflection
  public Iterable<Symbol> getSymbols(Predicate<Symbol> predicate) {
    return (Iterable<Symbol>) invoke(getSymbols, maybeAsFilter(predicate));
  }

  @SuppressWarnings("unchecked") // reflection
  public Iterable<Symbol> getSymbols(Predicate<Symbol> predicate, LookupKind lookupKind) {
    return (Iterable<Symbol>) invoke(getSymbolsLookupKind, maybeAsFilter(predicate), lookupKind);
  }

  public boolean anyMatch(Predicate<Symbol> predicate) {
    return (boolean) invoke(anyMatch, maybeAsFilter(predicate));
  }

  private static final Class<?> FILTER_CLASS = getFilterClass();

  private static @Nullable Class<?> getFilterClass() {
    if (RuntimeVersion.isAtLeast17()) {
      return null;
    }
    try {
      return Class.forName("com.sun.tools.javac.util.Filter");
    } catch (ClassNotFoundException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private static final Method anyMatch = getImpl("anyMatch", Predicate.class);

  private static final Method getSymbolsByName =
      getImpl("getSymbolsByName", Name.class, Predicate.class);

  private static final Method getSymbolsByNameLookupKind =
      getImpl("getSymbolsByName", Name.class, Predicate.class, LookupKind.class);

  private static final Method getSymbols = getImpl("getSymbols", Predicate.class);

  private static final Method getSymbolsLookupKind =
      getImpl("getSymbols", Predicate.class, LookupKind.class);

  private static Method getImpl(String name, Class<?>... parameters) {
    return FILTER_CLASS != null
        ? getMethodOrDie(
            Scope.class,
            name,
            Arrays.stream(parameters)
                .map(p -> p.equals(Predicate.class) ? FILTER_CLASS : p)
                .toArray(Class<?>[]::new))
        : getMethodOrDie(Scope.class, name, parameters);
  }

  private final Scope scope;

  ErrorProneScope(Scope scope) {
    this.scope = scope;
  }

  private Object invoke(Method method, Object... args) {
    try {
      return method.invoke(scope, args);
    } catch (ReflectiveOperationException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }

  private Object maybeAsFilter(Predicate<Symbol> predicate) {
    if (FILTER_CLASS == null) {
      return predicate;
    }
    return Proxy.newProxyInstance(
        getClass().getClassLoader(),
        new Class<?>[] {FILTER_CLASS},
        new InvocationHandler() {
          @Override
          public Object invoke(Object proxy, Method method, Object[] args) {
            checkState(method.getName().equals("accepts"));
            return predicate.test((Symbol) args[0]);
          }
        });
  }

  private static Method getMethodOrDie(Class<?> clazz, String name, Class<?>... parameters) {
    try {
      return clazz.getMethod(name, parameters);
    } catch (NoSuchMethodException e) {
      throw new LinkageError(e.getMessage(), e);
    }
  }
}
