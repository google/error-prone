import com.google.common.base.Functions;
import com.google.common.base.Predicates;

/**
 * Test case for fully qualified method call.
 * @author eaftan@google.com (Eddie Aftandilian)
 */
public class PositiveCase2 {
  public void error() {
    com.google.common.base.Preconditions.checkNotNull("string literal");
  }
}