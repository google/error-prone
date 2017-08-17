---
title: OvershadowingSubclassFields
summary: Overshadowing variables of superclass causes confusion and errors
layout: bugpattern
tags: ''
severity: WARNING
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: hiding_

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("OvershadowingSubclassFields")` annotation to the enclosing element.

----------

### Positive examples
__OvershadowingSubclassFieldsPositiveCases1.java__

{% highlight java %}
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

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class OvershadowingSubclassFieldsPositiveCases1 {

  /** base class */
  public static class ClassA {
    protected String varOne;
    public int varTwo;
    String varThree;
  }


  /** ClassB has a variable name as parent */
  public static class ClassB extends ClassA {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    private String varOne = "Test";
  }

  /** ClassC has same variable name as grandparent */
  public static class ClassC extends ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    public int varTwo;
  }

  /** ClassD has same variable name as grandparent and other unrelated members */
  public static class ClassD extends ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    protected int varThree;
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    int varTwo;
    String randOne;
    String randTwo;
  }

  /** ClassE has same variable name as grandparent */
  public static class ClassE extends ClassC {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassC
    public String varTwo;
  }

  public static class ClassF extends ClassA {
    @SuppressWarnings("OvershadowingSubclassFields") // no warning because it's suppressed
    public String varThree;
  }

  public static class ClassG extends ClassF {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassF
    String varThree;
  }
}
{% endhighlight %}

__OvershadowingSubclassFieldsPositiveCases2.java__

{% highlight java %}
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

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class OvershadowingSubclassFieldsPositiveCases2 {

  /**
   * ClassA extends a class from a different file and ClassA has a member with the same name as its
   * parent
   */
  public class ClassA extends OvershadowingSubclassFieldsPositiveCases1.ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    private int varTwo;
  }

  /**
   * ClassB extends a class from a different file and ClassB has a member with the same name as its
   * grandparent
   */
  public class ClassB extends OvershadowingSubclassFieldsPositiveCases1.ClassB {
    // BUG: Diagnostic contains: Overshadowing variables of superclass causes confusion and errors.
    // This variable is overshadowing a variable in superclass:  ClassA
    public int varOne = 2;
  }
}
{% endhighlight %}

### Negative examples
__OvershadowingSubclassFieldsNegativeCases.java__

{% highlight java %}
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

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
public class OvershadowingSubclassFieldsNegativeCases {
  // base class
  static class ClassA {
    public int varOne;
  }

  // subclass with member variables of different names
  static class ClassB extends ClassA {
    private String varTwo;
    private int varThree;
    public static int varFour;
    public int varFive;
  }

  // subclass with initialized member variable of different name
  static class ClassC extends ClassB {
    private String varFour = "Test";

    // warning suppressed when overshadowing variable in parent
    @SuppressWarnings("OvershadowingSubclassFields")
    public int varFive;

    // warning suppressed when overshadowing variable in grandparent
    @SuppressWarnings("OvershadowingSubclassFields")
    public int varOne;
  }

  // subclass with member *methods* with the same name as superclass member variable -- this is ok
  static class ClassD extends ClassC {
    public void varThree() {}

    public void varTwo() {}
  }
}
{% endhighlight %}

