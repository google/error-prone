// Copyright 2011 Google Inc. All Rights Reserved.

package preconditions_expensive_string;

import com.google.common.base.Preconditions;

/**
 * Preconditions calls which shouldn't be picked up for expensive string operations
 * 
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class NegativeCase1 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo %s foo  is not a good foo", foo);

    // This call should not be converted because of the %d, which does some locale specific
    // behaviour. If it were an %s, it would be fair game.
    Preconditions.checkState(true, String.format("The foo %d foo is not a good foo", foo));
  }
}
