---
title: ClassCanBeStatic
summary: Inner class is non-static but does not reference enclosing class
layout: bugpattern
category: JDK
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
An inner class should be static unless it references membersof its enclosing class. An inner class that is made non-static unnecessarilyuses more memory and does not make the intent of the class clear.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("ClassCanBeStatic")` annotation to the enclosing element.

----------

### Positive examples
__ClassCanBeStaticPositiveCase1.java__

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
 * @author alexloh@google.com (Alex Loh)
 */
public class ClassCanBeStaticPositiveCase1 {
  
  int outerVar;

  // Non-static inner class that does not use outer scope
  // BUG: Diagnostic contains: static class Inner1
  class Inner1 {
    int innerVar;
  }
}
{% endhighlight %}

__ClassCanBeStaticPositiveCase2.java__

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
 * @author alexloh@google.com (Alex Loh)
 */
public class ClassCanBeStaticPositiveCase2 {

  int outerVar1;
  int outerVar2;

  // Outer variable overridden
  // BUG: Diagnostic contains: private /* COMMENT */ static final class Inner2
  private /* COMMENT */ final class Inner2 {
    int outerVar1;
    int innerVar = outerVar1;
    int localMethod(int outerVar2) {
      return outerVar2;
    }
  }
}
{% endhighlight %}

__ClassCanBeStaticPositiveCase3.java__

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
 * @author alexloh@google.com (Alex Loh)
 */
public class ClassCanBeStaticPositiveCase3 {

  static int outerVar;

  // Nested non-static inner class inside a static inner class
  static class NonStaticOuter {
    int nonStaticVar = outerVar;
    // BUG: Diagnostic contains: public static class Inner3
    public class Inner3 {
    }
  }
}
{% endhighlight %}

### Negative examples
__ClassCanBeStaticNegativeCases.java__

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
 * @author alexloh@google.com (Alex Loh)
 */
public class ClassCanBeStaticNegativeCases {
  int outerVar;
  public int outerMethod() {
    return 0;
  }

  public static class Inner1 { // inner class already static
    int innerVar;
  }

  public class Inner2 { // inner class references an outer variable
    int innerVar = outerVar;
  }

  public class Inner3 { // inner class references an outer variable in a method
    int localMethod() {
      return outerVar;
    }
  }

  public class Inner4 { // inner class references an outer method in a method
    int localMethod() {
      return outerMethod();
    }
  }

  // outer class is a nested but non-static, and thus cannot have a static class
  class NonStaticOuter {
    int nonStaticVar = outerVar;
    class Inner5 {
    }
  }

  // inner class is local and thus cannot be static
  void foo() {
    class Inner6 {
    }
  }

  // inner class is anonymous and thus cannot be static
  Object bar() {
    return new Object() {
    };
  }

  // enums are already static
  enum Inner7 {
    RED,
    BLUE,
    VIOLET,
  }

  // outer class is a nested but non-static, and thus cannot have a static class
  void baz() {
    class NonStaticOuter2 {
      int nonStaticVar = outerVar;
      class Inner8 {
      }
    }
  }

  // inner class references a method from inheritance
  public static interface OuterInter {
    int outerInterMethod();
  }
  abstract static class AbstractOuter implements OuterInter {
    class Inner8 {
      int localMethod() {
        return outerInterMethod();
      }
    }
  }
}
{% endhighlight %}

