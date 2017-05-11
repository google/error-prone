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

package com.google.errorprone.bugpatterns.android.testdata;

import android.app.Fragment;

/** @author avenet@google.com (Arnaud J. Venet) */
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
