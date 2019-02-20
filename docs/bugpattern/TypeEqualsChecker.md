[`TypeMirror`](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/lang/model/type/TypeMirror.html)
doesn't override `Object.equals` and instances are not interned by javac, so
testing types for equality should be done with
[`Types#isSameType`](https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/lang/model/util/Types.html#isSameType\(javax.lang.model.type.TypeMirror,javax.lang.model.type.TypeMirror\))
instead.

If you're implementing an Error Prone `BugChecker`, you can get a `Types`
instance from `VisitorState`.

If you're implementing `AnnotationProcessor`, you can get the `Types` instance
from `javax.annotation.processing.ProcessingEnvironment`.
