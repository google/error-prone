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

package com.google.errorprone.bugpatterns.android.testdata;

/** @author jasonlong@google.com (Jason Long) */
public class CustomFragmentNotInstantiablePositiveCases {
  // BUG: Diagnostic contains: public
  static class PrivateFragment extends CustomFragment {
    public PrivateFragment() {}
  }

  public static class PrivateConstructor extends CustomFragment {
    // BUG: Diagnostic contains: public
    PrivateConstructor() {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class NoConstructor extends CustomFragment {
    public NoConstructor(int x) {}
  }

  // BUG: Diagnostic contains: nullary constructor
  public static class NoConstructorV4 extends android.support.v4.app.Fragment {
    public NoConstructorV4(int x) {}
  }

  public static class ParentFragment extends CustomFragment {
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
    public class InnerFragment extends CustomFragment {
      public InnerFragment() {}
    }

    public CustomFragment create1() {
      // BUG: Diagnostic contains: public
      return new CustomFragment() {};
    }

    public CustomFragment create2() {
      // BUG: Diagnostic contains: public
      class LocalFragment extends CustomFragment {}
      return new LocalFragment();
    }
  }
}
