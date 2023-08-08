`Optional` is a container object which may or may not contain a value. The
presence or absence of the contained value should be demonstrated by the
`Optional` object itself.

Using an Optional variable which is expected to possibly be null is discouraged.
An nullable Optional which uses `null` to indicate the absence of the value will
lead to extra work for `null` checking when using the object and even cause
exceptions such as `NullPointerException`. It is best to indicate the absence of
the value by assigning it an empty optional.
