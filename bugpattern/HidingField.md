---
title: HidingField
summary: Hiding fields of superclasses may cause confusion and errors
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: hiding, OvershadowingSubclassFields_

## The problem
If a class has a field of the same name as any field visible to it on any of its
superclasses or superinterfaces, the subclass' field is said to "[hide]
(https://docs.oracle.com/javase/tutorial/java/IandI/hidevariables.html)" the
superclass' field.

When this circumstance occurs, users of the class declaring the hiding field
can't interact with the fields from the superclass.

Let's take a look at how field hiding might cause problems:

```java
class Super {
  public String foo = "bar";
}

class Sub extends Super {
  private int foo = 0; // the same name, so this hides `Super`'s `foo`
}

class Main {
  void stringFn(String s) { /*...*/ }
  public static void main(String... args) {
    // Looking at the API of `Super`, I should be able to access a string `foo`
    // on any object of type `Super` or its subclasses, right?
    stringFn(new Sub().foo); // Oops! `foo` is not visible, and the wrong type!
  }
}
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("HidingField")` to the enclosing element.

----------

### Positive examples
__HidingFieldPositiveCases1.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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
public class HidingFieldPositiveCases1 {

  /** base class */
  public static class ClassA {
    protected String varOne;
    public int varTwo;
    String varThree;
  }


  /** ClassB has a variable name as parent */
  public static class ClassB extends ClassA {
    // BUG: Diagnostic contains: superclass: ClassA
    private String varOne = "Test";
  }

  /** ClassC has same variable name as grandparent */
  public static class ClassC extends ClassB {
    // BUG: Diagnostic contains: superclass: ClassA
    public int varTwo;
  }

  /** ClassD has same variable name as grandparent and other unrelated members */
  public static class ClassD extends ClassB {
    // BUG: Diagnostic contains: superclass: ClassA
    protected int varThree;
    // BUG: Diagnostic contains: superclass: ClassA
    int varTwo;
    String randOne;
    String randTwo;
  }

  /** ClassE has same variable name as grandparent */
  public static class ClassE extends ClassC {
    // BUG: Diagnostic contains: superclass: ClassC
    public String varTwo;
  }

  public static class ClassF extends ClassA {
    @SuppressWarnings("HidingField") // no warning because it's suppressed
    public String varThree;
  }

  public static class ClassG extends ClassF {
    // BUG: Diagnostic contains: superclass: ClassF
    String varThree;
  }
}
{% endhighlight %}

__HidingFieldPositiveCases2.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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
public class HidingFieldPositiveCases2 {

  /**
   * ClassA extends a class from a different file and ClassA has a member with the same name as its
   * parent
   */
  public class ClassA extends HidingFieldPositiveCases1.ClassB {
    // BUG: Diagnostic contains: superclass: ClassA
    private int varTwo;
  }

  /**
   * ClassB extends a class from a different file and ClassB has a member with the same name as its
   * grandparent
   */
  public class ClassB extends HidingFieldPositiveCases1.ClassB {
    // BUG: Diagnostic contains: superclass: ClassA
    public int varOne = 2;
  }
}
{% endhighlight %}

### Negative examples
__HidingFieldNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2017 The Error Prone Authors.
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
public class HidingFieldNegativeCases {
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
    // publicly-visible static members in superclasses are pretty uncommon, and generally
    // referred to by qualification, so this 'override' is OK
    private String varFour = "Test";

    // The supertype's visibility is private, so this redeclaration is OK.
    private int varThree;

    // warning suppressed when overshadowing variable in parent
    @SuppressWarnings("HidingField")
    public int varFive;

    // warning suppressed when overshadowing variable in grandparent
    @SuppressWarnings("HidingField")
    public int varOne;
  }

  // subclass with member *methods* with the same name as superclass member variable -- this is ok
  static class ClassD extends ClassC {
    public void varThree() {}

    public void varTwo() {}
  }
}
{% endhighlight %}

