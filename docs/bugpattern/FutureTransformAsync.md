The usage of `transformAsync`, `callAsync` and `submitAsync` is not necessary
when all the return values of the transformation function are immediate futures.
In this case, the usage of `transform`, `call` and `submit` is preferred.

Note that `transform` cannot be used if the body of the transformation function
throws checked exceptions.
