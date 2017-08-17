---
title: FallThrough
summary: Switch case may fall through
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: fallthrough_

## The problem
The [Google Java Style Guide §4.8.4.2][style] requires that within a switch
block, each statement group either terminates abruptly (with a `break`,
`continue`, `return` or `throw` statement), or is marked with a comment to
indicate that execution will or might continue into the next statement group.
This special comment is not required in the last statement group of the switch
block.

Example:

```java
switch (input) {
  case 1:
  case 2:
    prepareOneOrTwo();
    // fall through
  case 3:
    handleOneTwoOrThree();
    break;
  default:
    handleLargeNumber(input);
}
```

[style]: https://google.github.io/styleguide/javaguide.html#s4.8.4-switch

## Suppression
Suppress false positives by adding an `@SuppressWarnings("FallThrough")` annotation to the enclosing element.

----------

### Positive examples
__FallThroughPositiveCases.java__

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

public class FallThroughPositiveCases {

  class NonTerminatingTryFinally {

    public int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            if (z > 0) {
              return i;
            } else {
              z++;
            }
          } finally {
            z++;
          }
          // BUG: Diagnostic contains:
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  abstract class TryWithNonTerminatingCatch {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            return bar();
          } catch (RuntimeException e) {
            log(e);
            throw e;
          } catch (Exception e) {
            log(e); // don't throw
          }
          // BUG: Diagnostic contains:
        case 1:
          return -1;
        default:
          return 0;
      }
    }

    abstract int bar() throws Exception;

    void log(Throwable e) {}
  }

  public class Tweeter {

    public int numTweets = 55000000;

    public int everyBodyIsDoingIt(int a, int b) {
      switch (a) {
        case 1:
          System.out.println("1");
          // BUG: Diagnostic contains:
        case 2:
          System.out.println("2");
          // BUG: Diagnostic contains:
        default:
      }
      return 0;
    }
  }
}
{% endhighlight %}

### Negative examples
__FallThroughNegativeCases.java__

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

import java.io.FileInputStream;
import java.io.IOException;

public class FallThroughNegativeCases {

  public class AllowAnyComment {

    public int numTweets = 55000000;

    public int everyBodyIsDoingIt(int a, int b) {
      switch (a) {
        case 1:
          System.out.println("1");
          // fall through
        case 2:
          System.out.println("2");
          break;
        default:
      }
      return 0;
    }
  }

  static class EmptyDefault {

    static void foo(String s) {
      switch (s) {
        case "a":
        case "b":
          throw new RuntimeException();
        default:
          // do nothing
      }
    }

    static void bar(String s) {
      switch (s) {
        default:
      }
    }
  }

  class TerminatedSynchronizedBlock {

    private final Object o = new Object();

    int foo(int i) {
      switch (i) {
        case 0:
          synchronized (o) {
            return i;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  class TryWithNonTerminatingFinally {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            return i;
          } finally {
            z++;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }

  abstract class TryWithTerminatingCatchBlocks {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            return bar();
          } catch (RuntimeException e) {
            log(e);
            throw e;
          } catch (Exception e) {
            log(e);
            throw new RuntimeException(e);
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }

    int tryWithResources(String path, int i) {
      switch (i) {
        case 0:
          try (FileInputStream f = new FileInputStream(path)) {
            return f.read();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        case 1:
          try (FileInputStream f = new FileInputStream(path)) {
            return f.read();
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        default:
          throw new RuntimeException("blah");
      }
    }

    abstract int bar() throws Exception;

    void log(Throwable e) {}
  }

  class TryWithTerminatingFinally {

    int foo(int i) {
      int z = 0;
      switch (i) {
        case 0:
          try {
            z++;
          } finally {
            return i;
          }
        case 1:
          return -1;
        default:
          return 0;
      }
    }
  }
}
{% endhighlight %}

