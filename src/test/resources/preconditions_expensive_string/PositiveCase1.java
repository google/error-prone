package preconditions_expensive_string;

import com.google.common.base.Preconditions;


/**
 * Test for method call involving String.format() and %s 
 *
 * @author sjnickerson@google.com (Simon Nickerson)
 */
public class PositiveCase1 {
  public void error() {
    int foo = 42;
    int bar = 78;
    Preconditions.checkState(true, String.format("The foo %s (%s) is not a good foo", foo, bar));
  }
}