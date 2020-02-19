

import javax.annotation.processing.Generated;

@Generated("com.google.auto.value.processor.AutoValueProcessor")
 final class AutoValue_Test extends Test {

  private final String valueOne;
  private final String valueTwo;

  AutoValue_Test(
      String valueOne,
      String valueTwo) {
    if (valueOne == null) {
      throw new NullPointerException("Null valueOne");
    }
    this.valueOne = valueOne;
    if (valueTwo == null) {
      throw new NullPointerException("Null valueTwo");
    }
    this.valueTwo = valueTwo;
  }

  @Override
  String valueOne() {
    return valueOne;
  }

  @Override
  String valueTwo() {
    return valueTwo;
  }

}
