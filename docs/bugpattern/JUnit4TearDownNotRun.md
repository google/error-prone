JUnit 3 provides the overridable method tearDown(), to be overridden by
subclasses when the test needs to perform some post-test de-initialization. In
JUnit 4, this is accomplished by annotating such a method with @After. The
method that triggered this error matches the definition of tearDown() from
JUnit3, but was not annotated with @After and thus won't be run by the JUnit4
runner.

If you intend for this tearDown() method not to be run by the JUnit4 runner, but
perhaps be manually invoked after certain test methods, please rename the method
or mark it private.

If the method is part of an abstract test class hierarchy where this class's
tearDown() is invoked by a superclass method that is annotated with @After, then
please rename the abstract method or add @After to the superclass's definition
of tearDown().
