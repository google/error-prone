`withSignature` relies on the string representation of internal classes in the
javac implementation. Those string representations are not necessarily stable
across versions of javac, and they can change when a method is annotated with
type-use annotations.

Additionally, `withSignature` currently has at least one undocumented behavioral
quirk.

The most reasonable use case for `withSignature` is for methods that declare or
use type variables, which are difficult or impossible to express with the rest
of the `MethodMatchers` API. Still, where practical, prefer to write your own
matching code instead of using `withSignature`.
