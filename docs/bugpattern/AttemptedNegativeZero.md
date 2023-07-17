Because `0` is an integer constant, `-0` is an integer, too. Integers have no
concept of "negative zero," so it is the same as plain `0`.

The value is then widened to a floating-point number. And while floating-point
numbers have a concept of "negative zero," the integral `0` is widened to the
floating-point "positive" zero.

To write a negative zero, you have to write a constant that is a floating-point
number. One simple way to do that is to write `-0.0`.
