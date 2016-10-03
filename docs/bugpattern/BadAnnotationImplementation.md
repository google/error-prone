Implementations of `java.lang.annotation.Annotation` must override `equals` and
`hashCode`, otherwise they inherit the implementations from `java.lang.Object`,
and those implementations do not meet the contract specified by the `Annotation`
interface.

It is very difficult to write these methods correctly, so consider using
[AutoAnnotation](https://github.com/google/auto/blob/master/value/src/main/java/com/google/auto/value/AutoAnnotation.java)
to generate the correct code automatically.
