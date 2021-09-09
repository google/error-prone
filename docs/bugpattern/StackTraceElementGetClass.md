`StackTraceElementGetClass#getClass` returns the Class object for
`StackTraceElement`. In almost all the scenarios this is not intended and is a
potential source of bugs. The most common usage of this method is to retrieve
the name of the class where exception occurred, in such cases
`StackTraceElement#getClassName` can be used instead. In case Class object for
`StackTraceElement` is required it can be obtained using
`StackTraceElement#class` method.
