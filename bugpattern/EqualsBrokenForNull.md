---
title: EqualsBrokenForNull
summary: equals() implementation throws NullPointerException when given null
layout: bugpattern
tags: ''
severity: WARNING
providesFix: REQUIRES_HUMAN_ATTENTION
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

## The problem


## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("EqualsBrokenForNull")` to the enclosing element.

----------

### Positive examples
__EqualsBrokenForNullPositiveCases.java__

{% highlight java %}
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
 * Positive test cases for EqualsBrokenForNull check.
 *
 * @author bhagwani@google.com (Sumit Bhagwani)
 */
public class EqualsBrokenForNullPositiveCases {

  private class ObjectGetClassArgToEquals {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!getClass().equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassArgToEqualsMultiLine {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!getClass().equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassArgToIsAssignableFrom {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!getClass().isAssignableFrom(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassArgToEquals2 {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!ObjectGetClassArgToEquals2.class.equals(obj.getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassReceiverToEquals {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!obj.getClass().equals(getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassReceiverToEquals2 {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!obj.getClass().equals(ObjectGetClassReceiverToEquals2.class)) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassReceiverToIsAssignableFrom {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (!obj.getClass().isAssignableFrom(getClass())) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassLeftOperandDoubleEquals {
    @Override
    // BUG: Diagnostic contains: if (other == null) { return false; }
    public boolean equals(Object other) {
      if (other.getClass() == ObjectGetClassLeftOperandDoubleEquals.class) {
        return true;
      }
      return false;
    }
  }

  private class ObjectGetClassRightOperandDoubleEquals {
    @Override
    // BUG: Diagnostic contains: if (other == null) { return false; }
    public boolean equals(Object other) {
      if (ObjectGetClassRightOperandDoubleEquals.class == other.getClass()) {
        return true;
      }
      return false;
    }
  }

  private class ObjectGetClassLeftOperandNotEquals {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (obj.getClass() != ObjectGetClassLeftOperandNotEquals.class) {
        return false;
      }
      return true;
    }
  }

  private class ObjectGetClassRightOperandNotEquals {
    @Override
    // BUG: Diagnostic contains: if (obj == null) { return false; }
    public boolean equals(Object obj) {
      if (ObjectGetClassRightOperandNotEquals.class != obj.getClass()) {
        return false;
      }
      return true;
    }
  }

  private class UnusedNullCheckWithNotEqualToInLeftOperand {
    @Override
    // BUG: Diagnostic contains: if (o == null) { return false; }
    public boolean equals(Object o) {
      if (this.getClass() != o.getClass() || o == null) {
        return false;
      }
      return true;
    }
  }

  private class UnusedNullCheckWithGetClassInEqualsArg {
    @Override
    // BUG: Diagnostic contains: if (o == null) { return false; }
    public boolean equals(Object o) {
      if (this.getClass().equals(o.getClass()) || o == null) {
        return false;
      }
      return true;
    }
  }
}
{% endhighlight %}

### Negative examples
__EqualsBrokenForNullNegativeCases.java__

{% highlight java %}
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
    @Override
    public boolean equals(Object other) {
      if (other instanceof UsesInstanceOfWithoutNullCheck) {
        return true;
      }
      return false;
    }
  }
}
{% endhighlight %}

