/*
 * Copyright 2018 The Error Prone Authors.
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

package com.google.errorprone.bugpatterns.nullness.testdata;

/**
 * Negative test cases for EqualsBrokenForNull check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class EqualsBrokenForNullNegativeCases {

  private class ExplicitNullCheckFirst {
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!getClass().equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class CheckWithSuperFirst {
    @Override
    public boolean equals(Object obj) {
      if (!super.equals(obj)) {
        return false;
      }
      if (!getClass().equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class NullCheckAndObjectGetClassNotEqualTo {
    @Override
    public boolean equals(Object o) {
      if (o == null || this.getClass() != o.getClass()) {
        return false;
      }
      return true;
    }
  }

  private class NullCheckAndObjectGetClassArgToEquals {
    @Override
    public boolean equals(Object obj) {
      if (obj != null && !getClass().equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class NullCheckAndObjectGetClassReceiverToEquals {
    @Override
    public boolean equals(Object obj) {
      if (obj != null && !obj.getClass().equals(getClass())) {
        return false;
      }
      return true;
    }
  }

  private class NullCheckAndObjectGetClassLeftOperandDoubleEquals {
    @Override
    public boolean equals(Object other) {
      if (other != null
          && other.getClass() == NullCheckAndObjectGetClassLeftOperandDoubleEquals.class) {
        return true;
      }
      return false;
    }
  }

  private class UsesInstanceOfWithNullCheck {
    @Override
    public boolean equals(Object other) {
      if (other != null && other instanceof UsesInstanceOfWithNullCheck) {
        return true;
      }
      return false;
    }
  }

  // https://stackoverflow.com/questions/2950319/is-null-check-needed-before-calling-instanceof
  private class UsesInstanceOfWithoutNullCheck {
    private int a;

    @Override
    public boolean equals(Object other) {
      if (other instanceof UsesInstanceOfWithoutNullCheck) {
        UsesInstanceOfWithoutNullCheck that = (UsesInstanceOfWithoutNullCheck) other;
        return that.a == a;
      }
      return false;
    }
  }

  private class IntermediateBooleanVariable {
    private int a;

    @Override
    public boolean equals(Object other) {
      boolean isEqual = other instanceof IntermediateBooleanVariable;
      if (isEqual) {
        IntermediateBooleanVariable that = (IntermediateBooleanVariable) other;
        return that.a == a;
      }
      return isEqual;
    }
  }

  private class UnsafeCastWithNullCheck {
    private int a;

    @Override
    public boolean equals(Object o) {
      if (o == null) {
        return false;
      }
      UnsafeCastWithNullCheck that = (UnsafeCastWithNullCheck) o;
      return that.a == a;
    }
  }
}
