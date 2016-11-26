This checker flags all equality tests between objects `o1` and `o2`, where `o1`
and `o2` have incompatible types and no common superclass of `o1` and `o2`
implements or declares an override of `java.lang.Object.equals()`. The result of
this test is almost certainly `false` in most configurations and a proper
equality predicate must be provided.

This check is enforced on all invocations of the following equality
predicates:

* `java.lang.Object.equals(Object)`
* `java.util.Objects.equals(Object, Object)`
* `com.google.common.base.Objects.equal(Object, Object)`

If you need to check that two objects are not equal for testing purposes,
we recommend that you use [`EqualsTester`][equalstester] instead.

[equalstester]: http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html
