---
title: FragmentNotInstantiable
summary: 'Subclasses of Fragment must be instantiable via Class#newInstance(): the
  class must be public, static and have a public nullary constructor'
layout: bugpattern
category: ANDROID
severity: WARNING
maturity: EXPERIMENTAL
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

_Alternate names: ValidFragment_

## The problem


## Suppression
Suppress false positives by adding an `@SuppressWarnings("FragmentNotInstantiable")` annotation to the enclosing element.

----------

### Positive examples
__FragmentNotInstantiablePositiveCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android;

import android.app.Fragment;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
public class FragmentNotInstantiablePositiveCases {
  // BUG: Diagnostic contains: public
  static class PrivateFragment extends Fragment {
    public PrivateFragment() {}
  }

  // BUG: Diagnostic contains: public
  static class PrivateV4Fragment extends android.support.v4.app.Fragment {
    public PrivateV4Fragment() {}
  }

  public static class PrivateConstructor extends Fragment {
    // BUG: Diagnostic contains: public
    PrivateConstructor() {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class NoConstructor extends Fragment {
    public NoConstructor(int x) {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class NoConstructorV4 extends android.support.v4.app.Fragment {
    public NoConstructorV4(int x) {}
  }

  public static class ParentFragment extends Fragment {
    public ParentFragment() {}
  }

  public static class ParentFragmentV4 extends android.support.v4.app.Fragment {
    public ParentFragmentV4() {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class DerivedFragmentNoConstructor extends ParentFragment {
    public DerivedFragmentNoConstructor(int x) {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class DerivedFragmentNoConstructorV4 extends ParentFragmentV4 {
    public DerivedFragmentNoConstructorV4(boolean b) {}
  }

  public class EnclosingClass {
    // BUG: Diagnostic contains: static
    public class InnerFragment extends Fragment {
      public InnerFragment() {}
    }

    public Fragment create1() {
      // BUG: Diagnostic contains: public
      return new Fragment() {};
    }

    public Fragment create2() {
      // BUG: Diagnostic contains: public
      class LocalFragment extends Fragment {}
      return new LocalFragment();
    }
  }
}
{% endhighlight %}

### Negative examples
__FragmentNotInstantiableNegativeCases.java__

{% highlight java %}
/*
 * Copyright 2016 Google Inc. All Rights Reserved.
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

package com.google.errorprone.bugpatterns.android;

import android.app.Fragment;

/**
 * @author avenet@google.com (Arnaud J. Venet)
 */
public class FragmentNotInstantiableNegativeCases {
  public static class NotAFragment1 {
    public NotAFragment1(int x) {}
  }

  public static class NotAFragment2 {
    private NotAFragment2() {}
  }

  private static class NotAFragment3 {}

  public class NotAFragment4 {}

  private abstract class AbstractFragment extends Fragment {
    public AbstractFragment(int x) {}
  }

  private abstract class AbstractV4Fragment extends android.support.v4.app.Fragment {
    private int a;

    public int value() {
      return a;
    }
  }

  public static class MyFragment extends Fragment {
    private int a;

    public int value() {
      return a;
    }
  }

  public static class DerivedFragment extends MyFragment {}

  public static class MyV4Fragment extends android.support.v4.app.Fragment {}

  public static class DerivedV4Fragment extends MyV4Fragment {
    private int a;

    public int value() {
      return a;
    }
  }

  public static class MyFragment2 extends Fragment {
    public MyFragment2() {}

    public MyFragment2(int x) {}
  }

  public static class DerivedFragment2 extends MyFragment2 {
    public DerivedFragment2() {}

    public DerivedFragment2(boolean b) {}
  }

  public static class EnclosingClass {
    public static class InnerFragment extends Fragment {
      public InnerFragment() {}
    }
  }

  interface AnInterface {
    public class ImplicitlyStaticInnerFragment extends Fragment {}

    class ImplicitlyStaticAndPublicInnerFragment extends Fragment {}
  }
}
{% endhighlight %}

