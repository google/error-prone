Declaring a top-level class whose binary name already exists on the compilation
bootclasspath or platform modules (`StandardLocation.PLATFORM_CLASS_PATH` or
`StandardLocation.SYSTEM_MODULES`) shadows the platform class during compilation
or at runtime depending on classloader delegation order, which can cause
unexpected linkage errors, duplicate class conflicts, or subtle behavioral
mismatches.
