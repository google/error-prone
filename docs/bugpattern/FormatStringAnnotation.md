To require validation of method parameters that will be used as printf style
format strings, annotate the parameters with `@FormatString` and their arguments
with `@FormatArg`. The accompanying Error Prone check will enforce that
parameters passed in as format strings must be one of:

* Another @FormatString variable
* A compile time constant string
* A final or effectively final variable assigned to a compile time constant
  string
* A string literal

It will then check that the format string is guaranteed to be valid assuming it
is passed the given `@FormatArg` parameters as format arguments. For more
information on possible format string errors, see
http://errorprone.info/bugpattern/FormatString
