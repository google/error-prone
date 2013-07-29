/*
 * Copyright 2011 Google Inc. All Rights Reserved.
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

package com.google.errorprone.matchers;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * In addition to matching an AST node, also gives a message and/or suggested fix.
 * @author alexeagle@google.com (Alex Eagle)
 */
public abstract class DescribingMatcher<T extends Tree> implements Matcher<T> {

  protected final String name;
  /**
   * A collection of IDs for this check, to be checked for in @SuppressWarnings annotations.
   */
  protected final Collection<String> names;
  protected final BugPattern annotation;

  public DescribingMatcher() {
    annotation = this.getClass().getAnnotation(BugPattern.class);
    if (annotation == null) {
      throw new IllegalStateException("Class " + this.getClass().getCanonicalName()
          + " not annotated with @BugPattern");
    }
    name = annotation.name();
    names = new ArrayList<String>(annotation.altNames().length + 1);
    names.add(name);
    names.addAll(Arrays.asList(annotation.altNames()));
  }

  /**
   * Generate the compiler diagnostic message based on information in the @BugPattern annotation.
   *
   * <p>If the formatSummary element of the annotation has been set, then use format string
   * substitution to generate the message.  Otherwise, just use the summary element directly.
   *
   * @param args Arguments referenced by the format specifiers in the annotation's formatSummary
   *     element
   * @return The compiler diagnostic message.
   */
  protected String getDiagnosticMessage(Object... args) {
    String summary;
    if (!annotation.formatSummary().isEmpty()) {
      if (args.length == 0) {
        throw new IllegalStateException("Compiler error message expects a format string, but "
            + "no arguments were provided");
      }
      summary = String.format(annotation.formatSummary(), args);
    } else {
      summary = annotation.summary();
    }
    return "[" + annotation.name() + "] " + summary + getLink();
  }

  private String getLink() {
    switch (annotation.linkType()) {
      case WIKI:
        return "\n  (see http://code.google.com/p/error-prone/wiki/" + annotation.name() + ")";
      case CUSTOM:
        // annotation.link() must be provided.
        if (annotation.link().isEmpty()) {
          throw new IllegalStateException("If linkType element of @BugPattern is CUSTOM, "
              + "a link element must also be provided.");
        }
        return  "\n  (see " + annotation.link() + ")";
      case NONE:
        return "";
      default:
        throw new IllegalStateException("Unexpected value for linkType element of @BugPattern: "
            + annotation.linkType());
    }
  }

  public String getName() {
    return name;
  }

  public Collection<String> getNames() {
    return names;
  }

  /**
   * Additional description of a matched AST node, useful in reporting the error or performing an
   * automated refactoring.
   * @param t an AST node which matched this matcher
   * @param state the shared state
   * @return the description
   */
  public abstract Description describe(T t, VisitorState state);

}
