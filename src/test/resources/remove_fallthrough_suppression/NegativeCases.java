// Copyright 2011 Google Inc. All Rights Reserved.

package remove_fallthrough_suppression;

/**
 * @author pepstein@google.com (Peter Epstein)
 */
@SuppressWarnings("unchecked")
public class NegativeCases {

  @SuppressWarnings("unchecked")
  public void suppressedMethod1a() {}

  @SuppressWarnings({"unchecked"})
  public void suppressedMethod1b() {}

  @SuppressWarnings({"varargs", "unchecked"})
  public void suppressedMethod2() {}
}
