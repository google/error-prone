package com.google.errorprone.bugpatterns;

/**
 * @author  cpovirk@google.com (Chris Povirk)
 */
public class IncompatibleEqualsAndHashcodePositiveCases {
  public static class HashCodeSkipsField {
    String a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof HashCodeSkipsField) {
        HashCodeSkipsField other = (HashCodeSkipsField) obj;
        return a.equals(other.a)
            && b == other.b;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return a.hashCode();
    }
  }

  public static class EqualsSkipsField {
    String a;
    int b;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof EqualsSkipsField) {
        EqualsSkipsField other = (EqualsSkipsField) obj;
        return a.equals(other.a);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return a.hashCode() * 17 + b;
    }
  }

  public static class FalsePositiveFromCachedHashCode {
    String a;
    int b;
    int cachedHashCode;

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof FalsePositiveFromCachedHashCode) {
        FalsePositiveFromCachedHashCode other = (FalsePositiveFromCachedHashCode) obj;
        return a.equals(other.a)
            && b == other.b;
      }
      return false;
    }

    @Override
    public int hashCode() {
      int local = cachedHashCode;
      if (local == 0) {
        cachedHashCode = local = a.hashCode() * 17 + b;
      }
      return local;
    }
  }
}
