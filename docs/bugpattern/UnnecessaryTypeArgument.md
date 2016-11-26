[JLS §15.12.2.1] allows non-generic methods to be invoked with type arguments:

[JLS §15.12.2.1]: https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2.1

> a non-generic method may be potentially applicable to an invocation that
> supplies explicit type arguments. Indeed, it may turn out to be applicable. In
> such a case, the type arguments will simply be ignored.
>
> This rule stems from issues of compatibility and principles of
> substitutability. Since interfaces or superclasses may be generified
> independently of their subtypes, we may override a generic method with a
> non-generic one. However, the overriding (non-generic) method must be
> applicable to calls to the generic method, including calls that explicitly
> pass type arguments. Otherwise the subtype would not be substitutable for its
> generified supertype.

There is no reason to do this in cases where the method being called is not an
override of a generic method.
