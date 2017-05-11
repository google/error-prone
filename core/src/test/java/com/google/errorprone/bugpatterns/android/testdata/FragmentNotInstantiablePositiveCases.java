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
