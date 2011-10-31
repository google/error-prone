package preconditions_expensive_string;

import com.google.common.base.Preconditions;

/**
 * Test for method call including string concatenation.
 * (Not yet supported, so this is a negative case)
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class NegativeCase2 {
  public void error() {
    int foo = 42;
    Preconditions.checkState(true, "The foo" + foo + " is not a good foo");
  }
}