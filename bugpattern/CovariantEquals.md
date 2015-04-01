---
title: CovariantEquals
layout: bugpattern
category: JDK
severity: ERROR
maturity: EXPERIMENTAL
---

<div style="float:right;"><table id="metadata">
<tr><td>Category</td><td>JDK</td></tr>
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Maturity</td><td>EXPERIMENTAL</td></tr>
</table></div>

# Bug pattern: CovariantEquals
__equals() method doesn't override Object.equals()__

## The problem
To be used by many libraries, an `equals` method must override `Object.equals`,which has a single parameter of type `java.lang.Object`. Defining a method which looks like `equals` but doesn't have the same signature is dangerous, since comparisons will have different results depending on which `equals` is called.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("CovariantEquals")` annotation to the enclosing element.

----------

# Examples
__CovariantEqualsNegativeCases.java__

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

import java.util.List;

/**
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CovariantEqualsNegativeCases {
  @Override
  public boolean equals(Object obj) {
    return false;
  }

  public boolean equals(CovariantEqualsNegativeCases other, String s) {
    return false;
  }

  public void equals(CovariantEqualsNegativeCases other) {
  }
  
  public List<Integer> equals(Integer other) {
    return null;
  }
}

class CovariantEqualsNegativeCase2 {
  @SuppressWarnings("CovariantEquals")
  public boolean equals(CovariantEqualsNegativeCase2 other) {
    return false;
  }
}

class AnotherClass {
  public boolean equals(CovariantEqualsNegativeCases other) {
    return false;
  }
  
  public int[] equals(int other) {
    return null;
  }
}

/**
 * Don't issue error when a class already overrides the real
 * equals. In this case covariant equals is probably a helper.
 */
class ClassWithEqualsOverridden {
  public boolean equals(Object other) {
    if (other instanceof ClassWithEqualsOverridden) {
      return equals((ClassWithEqualsOverridden)other);
    } else {
      return false;
    }
  }
  
  public boolean equals(ClassWithEqualsOverridden other) {
    return true;
  }
}

/**
 * Don't issue error when the covariant equals method is not public.
 * In that case it wasn't intended to override equals.
 */
class ClassWithNonPublicCovariantEquals {
  boolean equals(ClassWithNonPublicCovariantEquals other) {
    return true;
  }
}
{% endhighlight %}

__CovariantEqualsPositiveCase1.java__

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
 * @author alexeagle@google.com (Alex Eagle)
 */
public class CovariantEqualsPositiveCase1 {

  // BUG: Diagnostic contains: Did you mean '@Override'
  public boolean equals(CovariantEqualsPositiveCase1 other) {
    return false;
  }
}
{% endhighlight %}

__CovariantEqualsPositiveCase2.java__

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
 * @author Eddie Aftandilian(eaftan@google.com)
 */
public class CovariantEqualsPositiveCase2 {
  int i, j, k;
  
  // BUG: Diagnostic contains: Did you mean '@Override'
  public boolean equals(CovariantEqualsPositiveCase2 other) {
    if (i == other.i && j == other.j && k == other.k) {
      return true;
    }
    return false;
  }
}
{% endhighlight %}

__CovariantEqualsPositiveCase3.java__

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

import java.lang.String;

/**
 *
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CovariantEqualsPositiveCase3 {

  boolean isInVersion;
  String whitelist;

  // BUG: Diagnostic contains: Did you mean '@Override'
  public boolean equals(CovariantEqualsPositiveCase3 that) {
    return ((this.isInVersion == that.isInVersion) &&
            this.whitelist.equals(that.whitelist));
  }

}
{% endhighlight %}

__CovariantEqualsPositiveCase4.java__

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

import java.lang.String;

/**
 * Defining an equals method on an enum. Maybe this should be a separate kind of error?
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public enum CovariantEqualsPositiveCase4 {
  MERCURY,
  VENUS,
  EARTH,
  MARS,
  JUPITER,
  SATURN,
  URANUS,
  NEPTUNE,
  PLUTO;   // I don't care what they say, Pluto *is* a planet.
  
  // BUG: Diagnostic contains: remove this line
  public boolean equals(CovariantEqualsPositiveCase4 other) {
    return this == other;
  }
}
{% endhighlight %}

__CovariantEqualsPositiveCase5.java__

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

import java.lang.String;

/**
 * Defining an equals method with no body.
 * 
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class CovariantEqualsPositiveCase5 {
  
  // BUG: Diagnostic contains: @Override
  public native boolean equals(CovariantEqualsPositiveCase5 other);
}
{% endhighlight %}

