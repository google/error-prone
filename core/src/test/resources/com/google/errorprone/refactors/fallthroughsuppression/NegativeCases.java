// Copyright 2011 Google Inc. All Rights Reserved.

package removefallthroughsuppression;

/**
 * @author pepstein@google.com (Peter Epstein)
 */
@SuppressWarnings("unchecked")
public class NegativeCases extends ToBeExtended {

  @SuppressWarnings("unchecked")
  public void suppressedMethod1a() {}

  @SuppressWarnings({"unchecked"})
  public void suppressedMethod1b() {}

  @SuppressWarnings({"varargs", "unchecked"})
  public void suppressedMethod2() {}
  
  @Override
  public void overriddenMethod() {}
}
