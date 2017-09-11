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
