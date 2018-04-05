A method that overrides a @ForOverride method should not be invoked directly.
Instead, it should be invoked only from the class in which it was declared. For
example, if overriding Converter.doForward, you should invoke it through
Converter.convert. For testing, factor out the code you want to run to a
separate method.
