/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.suppliers.Suppliers.typeFromString;
import static com.google.errorprone.util.ASTHelpers.isSameType;

import com.google.errorprone.BugPattern;
import com.google.errorprone.BugPattern.StandardTags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.suppliers.Supplier;
import com.sun.tools.javac.code.Type;
import java.util.Optional;

/**
 * Points out on creation of Set and HashMap of type java.net.URL.
 *
 * <p>equals() and hashCode() of java.net.URL class make blocking internet connections.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
@BugPattern(
    summary = URLEqualsHashCode.MESSAGE,
    severity = WARNING,
    tags = StandardTags.FRAGILE_CODE)
public final class URLEqualsHashCode extends AbstractEqualsHashCodeUsageChecker {

  static final String MESSAGE =
      "Avoid hash-based containers of java.net.URL--the containers rely on equals() and"
          + " hashCode(), which cause java.net.URL to make blocking internet connections.";

  private static final String EQUALS_MESSAGE =
      "java.net.URL's equals() makes blocking internet connections; prefer java.net.URI.";

  @Override
  protected Optional<String> problem(Type type, Usage usage, VisitorState state) {
    if (!isSameType(type, JAVA_NET_URL.get(state), state)) {
      return Optional.empty();
    }
    return Optional.of(
        switch (usage) {
          case HASH_KEY -> MESSAGE;
          case EQUALS, ASSERTION, CONTAINMENT -> EQUALS_MESSAGE;
        });
  }

  private static final Supplier<Type> JAVA_NET_URL = typeFromString("java.net.URL");
}
