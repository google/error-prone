[AutoValue](https://github.com/google/auto/tree/master/value) instances should
be deeply immutable. Therefore, we recommend using immutable types for fields.
E.g., use `ImmutableMap` instead of `Map`, `ImmutableSet` instead of `Set`, etc.

Read more at
https://github.com/google/auto/blob/master/value/userguide/builders-howto.md#-use-a-collection-valued-property

Suppress violations by using `@SuppressWarnings("AutoValueImmutableFields")` on
the relevant `abstract` getter.
