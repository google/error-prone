---
title: AndroidInjectionBeforeSuper
summary: AndroidInjection.inject() should always be invoked before calling super.lifecycleMethod()
layout: bugpattern
tags: ''
severity: ERROR
providesFix: NO_FIX
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem
Members injection should always be called as early as possible to avoid uninitialized @Inject members. This is also crucial to protect against bugs during configuration changes and reattached Fragments to make sure that each framework type is injected in the appropriate order.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("AndroidInjectionBeforeSuper")` to the enclosing element.

----------

### Positive examples
__AndroidInjectionBeforeSuperPositiveCases.java__

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

package com.google.errorprone.bugpatterns.inject.dagger.testdata;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import dagger.android.AndroidInjection;

final class AndroidInjectionBeforeSuperPositiveCases {
  public class WrongOrder extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }

  public class StatementsInBetween extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      System.out.println("hello, world");
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }

  public static class BaseActivity extends Activity {}

  public class ExtendsBase extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }

  public class WrongOrderFragmentPreApi23 extends Fragment {
    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }

  public class WrongOrderFragment extends Fragment {
    @Override
    public void onAttach(Context context) {
      super.onAttach(context);
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }

  public class WrongOrderService extends Service {
    @Override
    public void onCreate() {
      super.onCreate();
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
      return null;
    }
  }
}
{% endhighlight %}

### Negative examples
__AndroidInjectionBeforeSuperNegativeCases.java__

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

package com.google.errorprone.bugpatterns.inject.dagger.testdata;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import dagger.android.AndroidInjection;

final class AndroidInjectionBeforeSuperNegativeCases {
  public class CorrectOrder extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      AndroidInjection.inject(this);
      super.onCreate(savedInstanceState);
    }
  }

  public class StatementsInBetween extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      AndroidInjection.inject(this);
      System.out.println("hello, world");
      super.onCreate(savedInstanceState);
    }
  }

  public static class BaseActivity extends Activity {}

  public class ExtendsBase extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      AndroidInjection.inject(this);
      super.onCreate(savedInstanceState);
    }
  }

  public static class Foo {
    public void onCreate(Bundle bundle) {}
  }

  public class FooActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
      new Foo().onCreate(savedInstanceState);
      AndroidInjection.inject(this);
      super.onCreate(savedInstanceState);
    }
  }

  public abstract class ActivityWithAbstractOnCreate extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {}

    public abstract void onCreate(Bundle savedInstanceState, boolean bar);
  }

  public class CorrectOrderFragment extends Fragment {
    @Override
    public void onAttach(Activity activity) {
      AndroidInjection.inject(this);
      super.onAttach(activity);
    }
  }

  public class CorrectOrderService extends Service {
    @Override
    public void onCreate() {
      AndroidInjection.inject(this);
      super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
      return null;
    }
  }
}
{% endhighlight %}

