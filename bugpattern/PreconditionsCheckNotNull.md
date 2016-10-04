---
title: PreconditionsCheckNotNull
summary: Literal passed as first argument to Preconditions.checkNotNull() can never be null
layout: bugpattern
category: GUAVA
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Preconditions.checkNotNull() takes two arguments. The first is the reference that should be non-null. The second is the error message to print (usually a string literal). Often the order of the two arguments is swapped, and the reference is never actually checked for nullity. This check ensures that the first argument to Preconditions.checkNotNull() is not a literal.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("PreconditionsCheckNotNull")` annotation to the enclosing element.

----------

### Positive examples
__PreconditionsCheckNotNullPositiveCase1.java__

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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;

public class PreconditionsCheckNotNullPositiveCase1 {
  public void error() {
    // BUG: Diagnostic contains: remove this line
    Preconditions.checkNotNull("string literal");
    String thing = null;
    // BUG: Diagnostic contains: (thing, 
    checkNotNull("thing is null", thing);
    // BUG: Diagnostic contains: 
    Preconditions.checkNotNull("a string literal " + "that's got two parts", thing);
  }
}
{% endhighlight %}

__PreconditionsCheckNotNullPositiveCase2.java__

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
 * Test case for fully qualified methodIs call.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PreconditionsCheckNotNullPositiveCase2 {
  public void error() {
    // BUG: Diagnostic contains: remove this line
    com.google.common.base.Preconditions.checkNotNull("string literal");
  }
}
{% endhighlight %}

__PreconditionsCheckNotNullPositiveCase3.java__

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

package com.google.errorprone.bugpatterns.testdata;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Test case for static import of Precondtions.checkNotNull.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PreconditionsCheckNotNullPositiveCase3 {
  public void error() {
    // BUG: Diagnostic contains: remove this line
    checkNotNull("string literal");
  }
}
{% endhighlight %}

### Negative examples
__PreconditionsCheckNotNullNegativeCase1.java__

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

public class PreconditionsCheckNotNullNegativeCase1 {
  public void go() {
    Preconditions.checkNotNull("this is ok");
  }
  
  private static class Preconditions {
    static void checkNotNull(String string) {
      System.out.println(string);
    }
  }
}
{% endhighlight %}

__PreconditionsCheckNotNullNegativeCase2.java__

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

import com.google.common.base.Preconditions;

public class PreconditionsCheckNotNullNegativeCase2 {
  public void go() {
    Object testObj = null;
    Preconditions.checkNotNull(testObj, "this is ok");
  }  
}
{% endhighlight %}

