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
import android.os.Bundle;
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

  public class WrongOrderFragment extends Fragment {
    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      // BUG: Diagnostic contains: AndroidInjectionBeforeSuper
      AndroidInjection.inject(this);
    }
  }
}
