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

import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.LookupKind;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;
import java.util.function.Predicate;

/**
 * A compatibility wrapper around {@code com.sun.tools.javac.util.Filter}
 *
 * @deprecated use {@link Scope} directly on JDK 17 and newer
 */
@Deprecated
public final class ErrorProneScope {

  public Iterable<Symbol> getSymbolsByName(Name name, Predicate<Symbol> predicate) {
    return scope.getSymbolsByName(name, predicate);
  }

  public Iterable<Symbol> getSymbolsByName(
      Name name, Predicate<Symbol> predicate, LookupKind lookupKind) {
    return scope.getSymbolsByName(name, predicate, lookupKind);
  }

  public Iterable<Symbol> getSymbols(Predicate<Symbol> predicate) {
    return scope.getSymbols(predicate);
  }

  public Iterable<Symbol> getSymbols(Predicate<Symbol> predicate, LookupKind lookupKind) {
    return scope.getSymbols(predicate, lookupKind);
  }

  public boolean anyMatch(Predicate<Symbol> predicate) {
    return scope.anyMatch(predicate);
  }

  private final Scope scope;

  ErrorProneScope(Scope scope) {
    this.scope = scope;
  }
}
