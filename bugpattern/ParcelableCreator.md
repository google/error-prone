---
title: ParcelableCreator
summary: Detects classes which implement Parcelable but don't have CREATOR
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
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ParcelableCreator")` to the enclosing element.

----------

### Positive examples
__ParcelableCreatorPositiveCases.java__

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

package com.google.errorprone.bugpatterns.android.testdata;

import android.os.Parcel;
import android.os.Parcelable;

/** @author bhagwani@google.com (Sumit Bhagwani) */
public class ParcelableCreatorPositiveCases {

  // BUG: Diagnostic contains: ParcelableCreator
  public static class ParcelableClassWithoutStaticCreator implements Parcelable {

    public final Parcelable.Creator<ParcelableClassWithoutStaticCreator> CREATOR =
        new Parcelable.Creator<ParcelableClassWithoutStaticCreator>() {
          public ParcelableClassWithoutStaticCreator createFromParcel(Parcel in) {
            return new ParcelableClassWithoutStaticCreator();
          }

          public ParcelableClassWithoutStaticCreator[] newArray(int size) {
            return new ParcelableClassWithoutStaticCreator[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }

  // BUG: Diagnostic contains: ParcelableCreator
  public static class ParcelableClassWithoutPublicCreator implements Parcelable {

    static final Parcelable.Creator<ParcelableClassWithoutPublicCreator> CREATOR =
        new Parcelable.Creator<ParcelableClassWithoutPublicCreator>() {
          public ParcelableClassWithoutPublicCreator createFromParcel(Parcel in) {
            return new ParcelableClassWithoutPublicCreator();
          }

          public ParcelableClassWithoutPublicCreator[] newArray(int size) {
            return new ParcelableClassWithoutPublicCreator[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }

  // BUG: Diagnostic contains: ParcelableCreator
  public static class ParcelableClassWithCreatorOfWrongType implements Parcelable {

    public static final Parcelable.Creator<ParcelableClassWithoutPublicCreator> CREATOR =
        new Parcelable.Creator<ParcelableClassWithoutPublicCreator>() {
          public ParcelableClassWithoutPublicCreator createFromParcel(Parcel in) {
            return new ParcelableClassWithoutPublicCreator();
          }

          public ParcelableClassWithoutPublicCreator[] newArray(int size) {
            return new ParcelableClassWithoutPublicCreator[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }
}
{% endhighlight %}

### Negative examples
__ParcelableCreatorNegativeCases.java__

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

package com.google.errorprone.bugpatterns.android.testdata;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.ClassLoaderCreator;
import com.google.errorprone.bugpatterns.android.ParcelableCreator;

/** @author bhagwani@google.com (Sumit Bhagwani) */
public class ParcelableCreatorNegativeCases {

  public static class PublicParcelableClass implements Parcelable {

    public static final Parcelable.Creator<PublicParcelableClass> CREATOR =
        new Parcelable.Creator<PublicParcelableClass>() {
          public PublicParcelableClass createFromParcel(Parcel in) {
            return new PublicParcelableClass();
          }

          public PublicParcelableClass[] newArray(int size) {
            return new PublicParcelableClass[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }

  public static class PublicParcelableClassWithClassLoaderCreator implements Parcelable {

    public static final ClassLoaderCreator<PublicParcelableClassWithClassLoaderCreator> CREATOR =
        new ClassLoaderCreator<PublicParcelableClassWithClassLoaderCreator>() {
          @Override
          public PublicParcelableClassWithClassLoaderCreator createFromParcel(
              Parcel source, ClassLoader loader) {
            return new PublicParcelableClassWithClassLoaderCreator();
          }

          @Override
          public PublicParcelableClassWithClassLoaderCreator createFromParcel(Parcel source) {
            return new PublicParcelableClassWithClassLoaderCreator();
          }

          @Override
          public PublicParcelableClassWithClassLoaderCreator[] newArray(int size) {
            return new PublicParcelableClassWithClassLoaderCreator[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }

  public interface EmptyInterface {}

  public static class EmptyInterfaceAndParcelableImpl implements EmptyInterface, Parcelable {
    public static final Parcelable.Creator<EmptyInterface> CREATOR =
        new Parcelable.Creator<EmptyInterface>() {
          public EmptyInterfaceAndParcelableImpl createFromParcel(Parcel in) {
            return new EmptyInterfaceAndParcelableImpl();
          }

          public EmptyInterfaceAndParcelableImpl[] newArray(int size) {
            return new EmptyInterfaceAndParcelableImpl[size];
          }
        };

    public int describeContents() {
      return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
      // no op
    }
  }
}
{% endhighlight %}

