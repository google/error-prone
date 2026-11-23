---
title: ParcelableCreator
summary: Detects classes which implement Parcelable but don't have CREATOR
layout: bugpattern
tags: ''
severity: ERROR
---

<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->


## The problem
Classes implementing
[`android.os.Parcelable`](https://developer.android.com/reference/android/os/Parcelable.html)
must also have a non-`null` static field called `CREATOR` of a type that
implements `Parcelable.Creator`.

Classes which don't follow this spec will compile fine but might not work in
Android runtime. Depending on platform, one will observe following crash at
runtime : `android.os.BadParcelableException: Parcelable protocol requires a
Parcelable.Creator object called CREATOR`

A typical example of correct implementation:

```
 public class MyParcelable implements Parcelable {
     private int data;

     public int describeContents() {
         return 0;
     }

     public void writeToParcel(Parcel out, int flags) {
         out.writeInt(data);
     }

     public static final Parcelable.Creator<MyParcelable> CREATOR
             = new Parcelable.Creator<MyParcelable>() {
         public MyParcelable createFromParcel(Parcel in) {
             return new MyParcelable(in);
         }

         public MyParcelable[] newArray(int size) {
             return new MyParcelable[size];
         }
     };

     private MyParcelable(Parcel in) {
         data = in.readInt();
     }
 }
```

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("ParcelableCreator")` to the enclosing element.
