---
title: SelfEquals
summary: An object is tested for equality to itself
layout: bugpattern
category: JDK
severity: WARNING
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to equals method are the same object, so it always returns true.
Either change the arguments to point to different objects or substitute true.

For test cases, instead of explicitly testing equals, use
[EqualsTester from Guava](http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html).

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SelfEquals")` annotation to the enclosing element.

----------

### Positive examples
__SelfEqualsPositiveCase.java__

{% highlight java %}
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

package com.google.errorprone.bugpatterns.testdata;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfEqualsPositiveCase {
  private String simpleField;

  public boolean test1(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
    return simpleField.equals(simpleField);
  }

  public boolean test2(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: simpleField.equals(other.simpleField);
    return simpleField.equals(this.simpleField);
  }

  public boolean test3(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
    return this.simpleField.equals(simpleField);
  }

  public boolean test4(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: this.simpleField.equals(other.simpleField);
    return this.simpleField.equals(this.simpleField);
  }

  public boolean test5(SelfEqualsPositiveCase obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    // BUG: Diagnostic contains: An object is tested for equality to itself
    return equals(this);
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase other = (SelfEqualsPositiveCase) obj;
    return simpleField.equals(((SelfEqualsPositiveCase)other).simpleField);
  }
}
{% endhighlight %}

### Negative examples
__SelfEqualsNegativeCases.java__

{% highlight java %}
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

package com.google.errorprone.bugpatterns.testdata;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SelfEqualsNegativeCases {
  private String field;

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }
  
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof SelfEqualsNegativeCases)) {
      return false;
    }
    
    SelfEqualsNegativeCases other = (SelfEqualsNegativeCases) o;
    return field.equals(other.field);
  }
  
  public boolean test() {
    return Boolean.TRUE.toString().equals(Boolean.FALSE.toString());
  }
}
{% endhighlight %}

