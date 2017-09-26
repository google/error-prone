Possible fixes:

*   If the field is never reassigned, add the missing `final` modifier.

*   If the field needs to be mutable, create a separate lock by adding a private
    final field and synchronizing on it to guard all accesses.

*   If the field is lazily initialized, annotation it with
    `com.google.errorprone.annotations.concurrent.LazyInit`.
