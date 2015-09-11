---
title: SelfEquals
summary: An object is tested for equality to itself
layout: bugpattern
category: GUAVA
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
The arguments to this equal method are the same object, so it always returns true.  Either change the arguments to point to different objects or substitute true.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("SelfEquals")` annotation to the enclosing element.

----------

## Examples
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

package com.google.errorprone.bugpatterns;

import com.google.common.base.Objects;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SelfEqualsNegativeCases {
  private String field;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SelfEqualsNegativeCases other = (SelfEqualsNegativeCases) o;
    return Objects.equal(field, other.field);
  }

  @Override
  public int hashCode() {
    return field != null ? field.hashCode() : 0;
  }
  
  public boolean equals2(Object o) {
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

__SelfEqualsPositiveCase1.java__

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

package com.google.errorprone.bugpatterns;

import com.google.common.base.Objects;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class SelfEqualsPositiveCase1 {
  private String field = "";

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SelfEqualsPositiveCase1 other = (SelfEqualsPositiveCase1) o;
    boolean retVal;
    // BUG: Diagnostic contains: Objects.equal(field, other.field)
    retVal = Objects.equal(field, field);
    // BUG: Diagnostic contains: Objects.equal(other.field, this.field)
    retVal &= Objects.equal(field, this.field);
    // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
    retVal &= Objects.equal(this.field, field);
    // BUG: Diagnostic contains: Objects.equal(this.field, other.field)
    retVal &= Objects.equal(this.field, this.field);
    
    return retVal;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(field);
  }
  
  public static void test() {
    ForTesting tester = new ForTesting();
    // BUG: Diagnostic contains: Objects.equal(tester.testing.testing, tester.testing)
    Objects.equal(tester.testing.testing, tester.testing.testing);
  }
  
  private static class ForTesting {
    public ForTesting testing;
    public String string;
  }
}
{% endhighlight %}

__SelfEqualsPositiveCase2.java__

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

package com.google.errorprone.bugpatterns;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class SelfEqualsPositiveCase2 {
  
  public boolean test1() {
    Object obj = new Object();
    // BUG: Diagnostic contains: true
    return obj.equals(obj);
  }
  
  private Object obj = new Object();
  public boolean test2() {
    // BUG: Diagnostic contains: true
    return obj.equals(this.obj);
  }
  
  public boolean test3() {
    // BUG: Diagnostic contains: true
    return equals(this);
  }
}
{% endhighlight %}

