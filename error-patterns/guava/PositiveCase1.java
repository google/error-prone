import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;

public class PositiveCase1 {
  public void error() {
    Preconditions.checkNotNull("string literal");
  }
}