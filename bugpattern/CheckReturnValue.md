---
title: CheckReturnValue
summary: Ignored return value of method that is annotated with @CheckReturnValue
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ResultOfMethodCallIgnored, ReturnValueIgnored_

## The problem
The JSR 305 `@CheckReturnValue` annotation marks methods whose return values
should be checked.  This error is triggered when one of these methods is called
but the result is not used.

`@CheckReturnValue` may be applied to a class or package to indicate that all
methods in that class or package must have their return values checked.  For
convenience, we provide an annotation, `@CanIgnoreReturnValue`, to exempt
specific methods or classes from this behavior.  `@CanIgnoreReturnValue` is
available from the Error Prone annotations package,
`com.google.errorprone.annotations`.

If you really want to ignore the return value of a method annotated with
`@CheckReturnValue`, a cleaner alternative to `@SuppressWarnings` is to assign
the result to a variable named `unused`:

```java
public void setNameFormat(String nameFormat) {
  String unused = format(nameFormat, 0); // fail fast if the format is bad or null
  this.nameFormat = nameFormat;
}
```

NOTE: `@CheckReturnValue` is ignored on calls from Mockito.verify and
Stubber.when(), e.g. `Mockito.verify(t).foo()` or `doReturn(val).when(t).foo()`
where `foo()` is annotated with `@CheckReturnValue`. This is because such tests
meant to check if a method is invoked, rather than consuming the return value.
We save people from adding `@SuppressWarnings` in these cases.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CheckReturnValue")` annotation to the enclosing element.

----------

### Positive examples
__CheckReturnValuePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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

import javax.annotation.CheckReturnValue;

/**
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CheckReturnValuePositiveCases {
  
  IntValue intValue = new IntValue(0);
  
  @CheckReturnValue
  private int increment(int bar) {
    return bar + 1;
  }
  
  public void foo() {
    int i = 1;
    // BUG: Diagnostic contains: remove this line
    increment(i);
    System.out.println(i);
  }
  
  public void bar() {
    // BUG: Diagnostic contains: this.intValue = this.intValue.increment()
    this.intValue.increment();
  }
  
  public void testIntValue() {
    IntValue value = new IntValue(10);
    // BUG: Diagnostic contains: value = value.increment()
    value.increment();
  }

  public void constructor() {
    /*
     * We may or may not want to treat this as a bug. On the one hand, the
     * subclass might be "using" the superclass, so it might not be being
     * "ignored." (Plus, it would be a pain to produce a valid suggested fix
     * that incorporates any subclass constructor body, which might even contain
     * calls to methods in the class.) On the other hand, the more likely
     * scenario may be a class like IteratorTester, which requires (a) that the
     * user subclass it to implement a method and (b) that the user call test()
     * on the constructed object. There, it would be nice if IteratorTester
     * could be annotated with @CheckReturnValue to mean "anyone who creates an
     * anonymous subclasses of this should still do something with that
     * subclass." But perhaps that's an abuse of @CheckForNull.
     *
     * Anyway, these tests are here to ensure that subclasses don't don't crash
     * the compiler.
     */
    new MyObject() {};

    class MySubObject1 extends MyObject {
    }

    class MySubObject2 extends MyObject {
      MySubObject2() {
      }
    }

    class MySubObject3 extends MyObject {
      MySubObject3() {
        super();
      }
    }

    // TODO(cpovirk): This one probably ought to be treated as a bug:
    new MyObject();
  }
  
  private class IntValue {
    final int i;
    
    public IntValue(int i) {
      this.i = i;
    }
    
    @CheckReturnValue
    public IntValue increment() {
      return new IntValue(i + 1);
    }
    
    public void increment2() {
      // BUG: Diagnostic contains: remove this line
      this.increment();
    }
    
    public void increment3() {
      // BUG: Diagnostic contains: remove this line
     increment();
    }
  }

  private static class MyObject {
    @CheckReturnValue
    MyObject() {}
  }

  private abstract static class LB1<A> {}
  private static class LB2<A> extends LB1<A> {
    
    @CheckReturnValue
    public static <T> LB2<T> lb1() {
        return new LB2<T>();
    }
    
    public static <T> LB2<T> lb2() {
      // BUG: Diagnostic contains: remove this line
      lb1();
      return lb1();
    }
  }
}
{% endhighlight %}

### Negative examples
__CheckReturnValueNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
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
public class CheckReturnValueNegativeCases {
  
  public void test1() {
    test2();
    Object obj = new String();
    obj.toString();
  }
  
  @SuppressWarnings("foo")  // wrong annotation
  public void test2() { 
  }
  
}
{% endhighlight %}

