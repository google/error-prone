/*
 * Copyright 2017 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.inject.dagger;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link AndroidInjectionBeforeSuper}. */
@RunWith(JUnit4.class)
public final class AndroidInjectionBeforeSuperTest {

  private final CompilationTestHelper compilationHelper =
      CompilationTestHelper.newInstance(AndroidInjectionBeforeSuper.class, getClass())
          .addSourceLines(
              "Activity.java",
              """
              package android.app;

              public class Activity {
                public void onCreate(android.os.Bundle bundle) {}
              }
              """)
          .addSourceLines(
              "Fragment.java",
              """
              package android.app;

              public class Fragment {
                public void onAttach(android.app.Activity activity) {}

                public void onAttach(android.content.Context context) {}
              }
              """)
          .addSourceLines(
              "Service.java",
              """
              package android.app;

              public class Service {
                public void onCreate() {}

                public android.os.IBinder onBind(android.content.Intent intent) {
                  return null;
                }
              }
              """)
          .addSourceLines(
              "Context.java",
              """
              package android.content;

              public class Context {}
              """)
          .addSourceLines(
              "Intent.java",
              """
              package android.content;

              public class Intent {}
              """)
          .addSourceLines(
              "Bundle.java",
              """
              package android.os;

              public class Bundle {}
              """)
          .addSourceLines(
              "IBinder.java",
              """
              package android.os;

              public interface IBinder {}
              """);

  @Test
  public void positiveCase() {
    compilationHelper
        .addSourceLines(
            "AndroidInjectionBeforeSuperPositiveCases.java",
            """
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
            """)
        .addSourceLines(
            "AndroidInjection.java",
"""
package dagger.android;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;

/**
 * Stub class for {@code dagger.android.AndroidInjection}. ErrorProne isn't an Android project and
 * can't depend on an {@code .aar} in Maven, so this is provided as a stub for testing.
 */
public final class AndroidInjection {
  public static void inject(Activity activity) {}

  public static void inject(Fragment fragment) {}

  public static void inject(Service service) {}
}
""")
        .doTest();
  }

  @Test
  public void negativeCase() {
    compilationHelper
        .addSourceLines(
            "AndroidInjectionBeforeSuperNegativeCases.java",
            """
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
            """)
        .addSourceLines(
            "AndroidInjection.java",
"""
package dagger.android;

import android.app.Activity;
import android.app.Fragment;
import android.app.Service;

/**
 * Stub class for {@code dagger.android.AndroidInjection}. ErrorProne isn't an Android project and
 * can't depend on an {@code .aar} in Maven, so this is provided as a stub for testing.
 */
public final class AndroidInjection {
  public static void inject(Activity activity) {}

  public static void inject(Fragment fragment) {}

  public static void inject(Service service) {}
}
""")
        .doTest();
  }
}
