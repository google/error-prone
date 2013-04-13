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

package com.google.errorprone;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * An annotation intended for implementations of Matcher which is picked up by our
 * documentation processor.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
@Retention(RUNTIME)
public @interface BugPattern {
  /**
   * @return ID of this bug, used in @SuppressWarnings. Should be unique
   */
  String name();

  /**
   * @return Alternate IDs for this bug, which may also be used in @SuppressWarnings
   */
  String[] altNames() default {};

  LinkType linkType() default LinkType.WIKI;

  /**
   * @return Used for the link if linkType() is CUSTOM.
   */
  String link() default "";

  /**
   * What link to show users in the javac error message, so they can learn more about the error
   */
  public enum LinkType {
    /**
     * Link to wiki, using the name identifier
     */
    WIKI,
    /**
     * Custom string
     */
    CUSTOM,
    /**
     * No link should be displayed
     */
    NONE
  }

  Category category();

  public enum Category {
    /**
     * general Java or JDK errors
     */
    JDK,
    /**
     * errors specific to Guava
     */
    GUAVA,
    /**
     * errors specific to Guice
     */
    GUICE,
    /**
     * errors specific to JUnit
     */
    JUNIT,
    /**
     * one-off refactorings that are not general errors
     */
    ONE_OFF
  }

  /**
   * Wiki syntax not allowed
   */
  String summary();

  /**
   * Wiki syntax allowed
   */
  String explanation();

  SeverityLevel severity();

  public enum SeverityLevel {
    ERROR,
    WARNING,
    /**
     * should not be used for general code
     */
    NOT_A_PROBLEM
  }

  MaturityLevel maturity();

  public enum MaturityLevel {
    MATURE,
    EXPERIMENTAL
  }

  public class Instance {
    public String name;
    public String summary;
    public String altNames;
    public MaturityLevel maturity;
    public SeverityLevel severity;
  }
}
