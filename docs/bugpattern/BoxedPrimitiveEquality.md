Comparison using reference equality instead of value equality. The inputs to
this comparison are boxed primitive types, where reference equality is
particularly bug-prone: Primitive wrapper classes cache instances for some (but
usually not all) values, so == may be equivalent to equals() for some values but
not others. Additionally, not all versions of the runtime and other libraries
use the cache in the same cases, so upgrades may change behavior. Furthermore,
reference identity is usually not useful for primitive wrappers, as they are
immutable types whose equals() method fully compares their values.
