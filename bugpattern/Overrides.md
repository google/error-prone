---
title: Overrides
summary: "Varargs doesn't agree for overridden method"
layout: bugpattern
category: JDK
severity: ERROR
maturity: MATURE
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: overrides_

## The problem
A varargs method is overridden by a method with an array parameter, or vice versa.  Please match the signature of the method being overridden.

## Suppression
Suppress false positives by adding an `@SuppressWarnings("Overrides")` annotation to the enclosing element.

----------

## Examples
__OverridesNegativeCase1.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesNegativeCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
    abstract void arrayMethod(Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    abstract void varargsMethod(final Object... newNames);
  }

  abstract class Child2 extends Base {
    @Override
    abstract void arrayMethod(Object[] xs);
  }

  static class StaticClass {
    static void staticVarargsMethod(Object... xs) {
    }
    
    static void staticArrayMethod(Object[] xs) {
    }
  }

  interface Interface {
    void varargsMethod(Object... xs);
    void arrayMethod(Object[] xs);
  }

  abstract class ImplementsInterface implements Interface {
    public abstract void varargsMethod(Object... xs);
    public abstract void arrayMethod(Object[] xs);
  }
}

// Varargs methods might end up overriding synthetic (e.g. bridge) methods, which will have already
// been lowered into a non-varargs form. Test that we don't report errors when a varargs method
// overrides a synthetic non-varargs method:

abstract class One {
  static class Builder {
    Builder varargsMethod(String... args) {
      return this;
    }
  }
}

class Two extends One {
  static class Builder extends One.Builder {
    @Override
    public Builder varargsMethod(String... args) {
      super.varargsMethod(args);
      return this;
    }
  }
}

class Three extends Two {
  static class Builder extends Two.Builder {
    @Override
    public Builder varargsMethod(String... args) {
      super.varargsMethod(args);
      return this;
    }
  }
}
{% endhighlight %}

__OverridesNegativeCase2.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesNegativeCase2 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubOne extends Base {
    @Override
    abstract void varargsMethod(Object... newNames);
  }
  
  abstract class SubTwo extends SubOne {
    @Override
    abstract void varargsMethod(Object... xs);
  }
  
  abstract class SubThree extends SubTwo {
    @Override
    abstract void varargsMethod(Object... newNames);
  }
}
{% endhighlight %}

__OverridesNegativeCase3.java__

{% highlight java %}
/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesNegativeCase3 {
  abstract class Base {
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubOne extends Base {
    @Override
    abstract void arrayMethod(Object[] xs);
  }
  
  abstract class SubTwo extends SubOne {
    @Override
    abstract void arrayMethod(Object[] xs);
  }
  
  abstract class SubThree extends SubTwo {
    @Override
    abstract void arrayMethod(Object[] xs);
  }
}
{% endhighlight %}

__OverridesPositiveCase1.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.ArrayList;
import java.util.List;

/**
 * This tests that the a bug is reported when a method override changes the type of a parameter
 * from varargs to array, or array to varargs. It also ensures that the implementation can
 * handles cases with multiple parameters, and whitespaces between the square brackets for
 * array types.
 * 
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase1 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
    abstract void arrayMethod(int x, Object[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void arrayMethod(int x, Object[] newNames);
    abstract void arrayMethod(int x, Object... newNames);
  }
  
  abstract class Child2 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[] xs);
  }
  
  abstract class Child3 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[  ] xs);
  }

  abstract class Child4 extends Base {
    @Override
    // BUG: Diagnostic contains: abstract void varargsMethod(Object... xs);
    abstract void varargsMethod(Object[                           ] xs);
  }

  abstract class Child5 extends Base {
    @Override
    // BUG: Diagnostic contains: Varargs
    abstract void varargsMethod(Object[/**/                       ] xs);
  }
  
  interface Interface {
    void varargsMethod(Object... xs);
    void arrayMethod(Object[] xs);
  }
  
  abstract class ImplementsInterface implements Interface {
    @Override
    // BUG: Diagnostic contains: 
    public abstract void varargsMethod(Object[] xs);
    @Override
    // BUG: Diagnostic contains: 
    public abstract void arrayMethod(Object... xs);
  }
 
  abstract class MyBase {
    abstract void f(Object... xs);
    abstract void g(Object[] xs);
  }
  
  interface MyInterface {
    void f(Object[] xs);
    void g(Object... xs);
  }
  
  abstract class ImplementsAndExtends extends MyBase implements MyInterface {
    // BUG: Diagnostic contains: 
    public abstract void f(Object... xs);
    // BUG: Diagnostic contains: 
    public abstract void g(Object[] xs);
  }
  
  abstract class ImplementsAndExtends2 extends MyBase implements MyInterface {
    // BUG: Diagnostic contains: 
    public abstract void f(Object[] xs);
    // BUG: Diagnostic contains: 
    public abstract void g(Object... xs);
  }
}
{% endhighlight %}

__OverridesPositiveCase2.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;


/**
 * This tests the case where there is a chain of method overrides where the varargs constraint is
 * not met, and the root is a varargs parameter.
 * TODO(user): The original implementation tried to be clever and make this consistent, but
 * didn't handle multiple interface inheritance.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase2 {
  abstract class Base {
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubOne extends Base {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object[] newNames);
  }

  abstract class SubTwo extends SubOne {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object... xs);
  }

  abstract class SubThree extends SubTwo {
    @Override
    // BUG: Diagnostic contains:
    abstract void varargsMethod(Object[] newNames);
  }
}
{% endhighlight %}

__OverridesPositiveCase3.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;


/**
 * This tests the case where there is a chain of method overrides where the varargs constraint is
 * not met, and the root has an array parameter.
 * TODO(user): The original implementation tried to be clever and make this consistent, but
 * didn't handle multiple interface inheritance.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase3 {
  abstract class Base {
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubOne extends Base {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object... newNames);
  }

  abstract class SubTwo extends SubOne {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object[] xs);
  }

  abstract class SubThree extends SubTwo {
    @Override
    // BUG: Diagnostic contains:
    abstract void arrayMethod(Object... newNames);
  }
}
{% endhighlight %}

__OverridesPositiveCase4.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.Map;

/**
 * Test that the suggested fix is correct in the presence of whitespace, comments.
 *
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase4 {

  @interface Note { }

  abstract class Base {
    abstract void varargsMethod(@Note final Map<Object, Object>... xs);
    abstract void arrayMethod(@Note final Map<Object, Object>[] xs);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: (@Note final Map<Object, Object> /* asd */ [] /* dsa */ xs);
    abstract void arrayMethod(@Note final Map<Object, Object> /* asd */ ... /* dsa */ xs);
  }

  abstract class Child2 extends Base {
    @Override
    //TODO(user): improve testing infrastructure so we can enforce that no fix is suggested.
    // BUG: Diagnostic contains: Varargs
    abstract void varargsMethod(@Note final Map<Object, Object>  /*dsa*/ [ /* [ */ ] /* dsa */ xs);
  }
}
{% endhighlight %}

__OverridesPositiveCase5.java__

{% highlight java %}
/*
 * Copyright 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.errorprone.bugpatterns;

import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
public class OverridesPositiveCase5 {

  abstract class Base {
    abstract void varargsMethod(Object[] xs, Object... ys);
    abstract void arrayMethod(Object[] xs, Object[] ys);
  }

  abstract class Child1 extends Base {
    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void arrayMethod(Object[] xs, Object[] ys);'
    abstract void arrayMethod(Object[] xs, Object... ys);

    @Override
    // BUG: Diagnostic contains: Did you mean 'abstract void varargsMethod(Object[] xs, Object... ys);'
    abstract void varargsMethod(Object[] xs, Object[] ys);

    void foo(Base base) {
      base.varargsMethod(null, new Object[] {}, new Object[] {}, new Object[] {}, new Object[] {});
    }
  }
}
{% endhighlight %}

