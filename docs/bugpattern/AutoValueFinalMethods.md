Consider that other developers will try to read and understand your value class
while looking only at your hand-written class, not the actual (generated)
implementation class. If you mark your concrete methods final, they won't have
to wonder whether the generated subclass might be overriding them. This is
especially helpful if you are underriding equals, hashCode or toString!

Reference: https://github.com/google/auto/blob/master/value/userguide/practices.md#mark-all-concrete-methods-final

NOTE: [Since `@Memoized` methods can't be final](https://github.com/google/auto/blob/master/value/userguide/howto.md#memoize_hash_tostring), the check doesn't flag them.
