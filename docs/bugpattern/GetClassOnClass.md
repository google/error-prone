Calling `getClass()` on an object of type Class returns the Class object for
java.lang.Class. Usually this is a mistake, and people intend to operate on the
object itself (for example, to print an error message). If you really did intend
to operate on the Class object for java.lang.Class, please use `Class.class`
instead for clarity.
