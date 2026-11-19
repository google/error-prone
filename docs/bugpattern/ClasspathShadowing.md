Declaring a top-level class whose binary name already exists on the compilation
classpath or module path (`StandardLocation.CLASS_PATH` or
`StandardLocation.MODULE_PATH`) shadows the dependency's class during
compilation, which causes duplicate class conflicts, compilation and runtime
classpath skew, or redundant compilation of the same source file across targets.

Depend on the library that already provides the class instead of recompiling its
source file or defining a duplicate class with the same binary name.
