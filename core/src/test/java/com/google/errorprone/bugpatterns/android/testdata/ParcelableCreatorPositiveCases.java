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
