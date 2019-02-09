A conditional expression with numeric second and third operands of differing
types may give surprising results.

For example:

    Object t = true ? Double.valueOf(0) : Integer.valueOf(0);
    System.out.println(t.getClass());  // class java.lang.Double

    Object f = false ? Double.valueOf(0) : Integer.valueOf(0);
    System.out.println(f.getClass());  // class java.lang.Double !!

Despite the apparent intent to get a `Double` in one case, and an `Integer` in
the other, the result is a `Double` in both cases.

This is because the rules in
[JLS § 15.25.2](https://docs.oracle.com/javase/specs/jls/se9/html/jls-15.html#jls-15.25.2)
state that differing numeric types will undergo binary numeric promotion. As
such, the latter case is evaluated as:

    Object f =
        Double.valueOf(
            false
                ? Double.valueOf(0).doubleValue()
                : (double) Integer.valueOf(0).intValue());

To get a different type in the two cases, one can either explicitly cast the
operands to a non-boxable type:

    Object f = false ? ((Object) Double.valueOf(0)) : ((Object) Integer.valueOf(0));
    System.out.println(t.getClass());  // class java.lang.Integer

Or use if/else:

    Object f;
    if (false) {
      f = Double.valueOf(0);
    } else {
      f = Integer.valueOf(0);
    }
