The usage of `transformAsync` and `callAsync` is not necessary when all the
return values of the transformation function are immediate futures. In this
case, the usage of `transform` and `call` is preferred.

Note that `transform` cannot be used if the body of the transformation function
throws checked exceptions.
