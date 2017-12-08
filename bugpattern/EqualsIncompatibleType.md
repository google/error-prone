---
title: EqualsIncompatibleType
summary: An equality test between objects with incompatible types always returns false
layout: bugpattern
tags: ''
severity: WARNING
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Consider the following code:

```java
String x = "42";
Integer y = 42;
if (x.equals(y)) {
  System.out.println("What is this, Javascript?");
} else {
  System.out.println("Types have meaning here.");
}
```

We understand that any `Integer` will *not* be equal to any `String`. However,
the signature of the `equals` method accepts any Object, so the compiler will
happily allow us to pass an Integer to the equals method. However, it will
always return false, which is probably not what we intended.

This check detects circumstances where the equals method is called when the two
objects in question can *never* be equal to each other. We check the following
equality methods:

* `java.lang.Object.equals(Object)`
* `java.util.Objects.equals(Object, Object)`
* `com.google.common.base.Objects.equal(Object, Object)`

## I'm trying to test to make sure my equals method works

Good! Many tests of equals methods neglect to test that equals on an unrelated
object return false.

We recommend using Guava's [EqualsTester][equalstester] to perform tests of your
equals method. Simply give it a collection of objects of your class, broken into
groups that should be equal to each other, and EqualsTester will ensure that:

*   Each object is equal to each other object in the same group as that object
*   Each object is equal to itself
*   Each object is unequal to all of the other objects not in the group
*   Each object is unequal to an unrelated object (Relevant to this check)
*   Each object is unequal to null
*   The `hashCode` of each object in a group is the same as the hash code of
    each other member of the group

Which should exhaustively check all of the properties of `equals` and
`hashCode`.

## But I'm doing something funky with my equals method!

The javadoc of [`Object.equals(Object)`][objeq] defines object equality very
precisely:

> The equals method implements an equivalence relation on non-null object
> references:
>
> It is reflexive: for any non-null reference value x, x.equals(x) should return
> true.
>
> It is symmetric: for any non-null reference values x and y, x.equals(y) should
> return true if and only if y.equals(x) returns true.
>
> It is transitive: for any non-null reference values x, y, and z, if
> x.equals(y) returns true and y.equals(z) returns true, then x.equals(z) should
> return true.
>
> It is consistent: for any non-null reference values x and y, multiple
> invocations of x.equals(y) consistently return true or consistently return
> false, provided no information used in equals comparisons on the objects is
> modified.
>
> For any non-null reference value x, x.equals(null) should return false.

TIP: [EqualsTester][equalstester] validates each of these properties.

For most simple value objects (e.g.: a `Point` containing `x` and `y`
coordinates), this generally means that the equals method will only return true
if the other object has the exact same class, and each of the components is
equal to the corresponding component in the other object. Here, there are
numerous tools in the Java ecosystem to generate the appropriate `equals` and
`hashCode` method implementations, including Guava's [AutoValue][av].

Another pattern often seen is to declare a common supertype with a defined
`equals` method (like `List`, which defines equality by having equal elements in
the same order). Then, different subclasses of that supertype (`LinkedList` and
`ArrayList`) can be equal to other classes with that supertype, since the
concrete class of the `List` is irrelevant. This checker will allow these types
of equality, as we detect when two objects share a common supertype with an
`equals` implementation and allow that to succeed.

Outside of these two general groups of equals methods, however, it's very
difficult to produce correctly-behaving equals methods. Most of the time, when
`equals` is implemented in a non-obvious manner, one or more of the properties
above isn't satisfied (generally the symmetric property). This can result in
subtle bugs, explained below.

### A bad example of `equals()`

```java
class Foo {
  private String foo; // Some property

  public boolean equals(Object other) {
    if (other instanceof String) {
      return other.equals(foo); // We want to be able to call equals with a String
    }
    if (other instanceof Foo) {
      return ((Foo) other).foo.equals(foo); // Simplified, avoid null checks
    }
    return false;
  }

  public int hashCode() {
    return foo.hashCode();
  }
}
```

Here, `Foo`'s equals method is defined to accept a `String` value in addition to
other `Foo`'s. This may appear to work at first, but you end up with some
complex situations:

```java
Foo a = new Foo("hello");
Foo b = new Foo("hello");
String hi = "hello";

if (a.equals(b)) {
  System.out.println("yes"); // Is printed, expected
}
if (b.equals(hi)) {
  System.out.println("yes"); // Is printed, abusing equals
}
if (hi.equals(b)) {
  System.out.println("no"); // Isn't printed, since String doesn't equals() Foo
}

Set<Foo> set = new HashSet<Foo>();
set.add(a);
set.add(b);

if (set.contains(hi)) {
  // Maybe? Depends on which way HashSet decides to call .equals()
  System.out.println("contained");
  // Is it removed? It's not guaranteed to be, since the .equals() method could
  // be called the other way in the remove path. Object.equals documentation
  // specifies it's supposed to be symmetric, so this could work.
  boolean removed = set.remove(hi);
}
```

[equalstester]: http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html
[objeq]: https://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#equals(java.lang.Object)
[av]: https://github.com/google/auto/blob/master/value/userguide/index.md

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsIncompatibleType")` to the enclosing element.

----------

### Positive examples
__EqualsIncompatibleTypePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** @author avenet@google.com (Arnaud J. Venet) */
public class EqualsIncompatibleTypePositiveCases {
  class A {}

  class B {}

  void checkEqualsAB(A a, B b) {
    // BUG: Diagnostic contains: incompatible types
    a.equals(b);
    // BUG: Diagnostic contains: incompatible types
    b.equals(a);
  }

  class C {}

  abstract class C1 extends C {
    public abstract boolean equals(Object o);
  }

  abstract class C2 extends C1 {}

  abstract class C3 extends C {}

  void checkEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c1);
    // BUG: Diagnostic contains: incompatible types
    c3.equals(c2);
    // BUG: Diagnostic contains: incompatible types
    c1.equals(c3);
    // BUG: Diagnostic contains: incompatible types
    c2.equals(c3);
  }

  void checkStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    java.util.Objects.equals(c2, c3);
  }

  void checkGuavaStaticEqualsCC1C2C3(C c, C1 c1, C2 c2, C3 c3) {
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c1);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c3, c2);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c1, c3);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(c2, c3);
  }

  interface I {
    public boolean equals(Object o);
  }

  class D {}

  class D1 extends D {}

  class D2 extends D implements I {}

  void checkEqualsDD1D2(D d, D1 d1, D2 d2) {
    // BUG: Diagnostic contains: incompatible types
    d1.equals(d2);
    // BUG: Diagnostic contains: incompatible types
    d2.equals(d1);
  }

  enum MyEnum {}

  enum MyOtherEnum {}

  void enumEquals(MyEnum m, MyOtherEnum mm) {
    // BUG: Diagnostic contains: incompatible types
    m.equals(mm);
    // BUG: Diagnostic contains: incompatible types
    mm.equals(m);

    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(m, mm);
    // BUG: Diagnostic contains: incompatible types
    com.google.common.base.Objects.equal(mm, m);
  }
}
{% endhighlight %}

### Negative examples
__EqualsIncompatibleTypeNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

/** @author avenet@google.com (Arnaud J. Venet) */
public class EqualsIncompatibleTypeNegativeCases {
  class A {
    public boolean equals(Object o) {
      if (o instanceof A) {
        return true;
      }
      return false;
    }
  }

  class B1 extends A {}

  class B2 extends A {}

  class B3 extends B2 {}

  void checkEqualsAB1B2B3(A a, B1 b1, B2 b2, B3 b3) {
    a.equals(a);
    a.equals(b1);
    a.equals(b2);
    a.equals(b3);
    a.equals(null);

    b1.equals(a);
    b1.equals(b1);
    b1.equals(b2);
    b1.equals(b3);
    b1.equals(null);

    b2.equals(a);
    b2.equals(b1);
    b2.equals(b2);
    b2.equals(b3);
    b2.equals(null);

    b3.equals(a);
    b3.equals(b1);
    b3.equals(b2);
    b3.equals(b3);
    b3.equals(null);
  }

  void checks(Object o, boolean[] bools, boolean bool) {
    o.equals(bool);
    o.equals(bools[0]);
  }

  void checkJUnit(B1 b1, B2 b2) {
    org.junit.Assert.assertFalse(b1.equals(b2));
  }

  void checkStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
    java.util.Objects.equals(a, a);
    java.util.Objects.equals(a, b1);
    java.util.Objects.equals(a, b2);
    java.util.Objects.equals(a, b3);
    java.util.Objects.equals(a, null);

    java.util.Objects.equals(b1, b3);
    java.util.Objects.equals(b2, b3);
    java.util.Objects.equals(b3, b3);
    java.util.Objects.equals(null, b3);
  }

  void checkGuavaStaticEquals(A a, B1 b1, B2 b2, B3 b3) {
    com.google.common.base.Objects.equal(a, a);
    com.google.common.base.Objects.equal(a, b1);
    com.google.common.base.Objects.equal(a, b2);
    com.google.common.base.Objects.equal(a, b3);
    com.google.common.base.Objects.equal(a, null);

    com.google.common.base.Objects.equal(b1, b3);
    com.google.common.base.Objects.equal(b2, b3);
    com.google.common.base.Objects.equal(b3, b3);
    com.google.common.base.Objects.equal(null, b3);
  }

  class C {}

  abstract class C1 extends C {
    public abstract boolean equals(Object o);
  }

  abstract class C2 extends C1 {}

  abstract class C3 extends C1 {}

  void checkEqualsC1C2C3(C1 c1, C2 c2, C3 c3) {
    c1.equals(c1);
    c1.equals(c2);
    c1.equals(c3);
    c1.equals(null);

    c2.equals(c1);
    c2.equals(c2);
    c2.equals(c3);
    c2.equals(null);

    c3.equals(c1);
    c3.equals(c2);
    c3.equals(c3);
    c3.equals(null);
  }

  interface I {
    public boolean equals(Object o);
  }

  class E1 implements I {}

  class E2 implements I {}

  class E3 extends E2 {}

  void checkEqualsIE1E2E3(I e, E1 e1, E2 e2, E3 e3) {
    e.equals(e);
    e.equals(e1);
    e.equals(e2);
    e.equals(e3);
    e.equals(null);

    e1.equals(e);
    e1.equals(e1);
    e1.equals(e2);
    e1.equals(e3);
    e1.equals(null);

    e2.equals(e);
    e2.equals(e1);
    e2.equals(e2);
    e2.equals(e3);
    e2.equals(null);

    e3.equals(e);
    e3.equals(e1);
    e3.equals(e2);
    e3.equals(e3);
    e3.equals(null);
  }

  interface J {}

  class F1 implements J {}

  abstract class F2 {
    public abstract boolean equals(J o);
  }

  void checkOtherEquals(F1 f1, F2 f2) {
    f2.equals(f1);
  }
}
{% endhighlight %}

