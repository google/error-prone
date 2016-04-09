BigDecimal has a two mechanisms for converting a double into a BigDecimal, new
BigDecimal(double), and BigDecimal.valueOf(double). These methods are a possible
source of precision loss if the number does not have an exact double
representation. The new BigDecimal(String) and new BigDecimal(long) constructors
should be prefered, as they do not require using a lossy argument.

For example `0.1` cannot be exactly represented a double. Thus
`new BigDecimal(.1)` represents the same bignum as
`new BigDecimal("0.1000000000000000055511151231257827021181583404541015625")`
and not `new BigDecimal(".1").
