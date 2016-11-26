The arguments to equals method are the same object, so it always returns true.
Either change the arguments to point to different objects or substitute true.

For test cases, instead of explicitly testing equals, use
[EqualsTester from Guava](http://static.javadoc.io/com.google.guava/guava-testlib/19.0/com/google/common/testing/EqualsTester.html).
