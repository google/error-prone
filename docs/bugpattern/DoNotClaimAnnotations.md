Do not 'claim' annotations in annotation processors; [`Processor#process`]
should unconditionally `return false`. Claiming annotations prevents other
processors from seeing the annotations, which there's usually no reason to do.
It's also fragile, since it relies on the order the processors run in, and
there's no robust way in most build systems to ensure a particular processor
sees the annotations first.

[`Processor#process`]: https://docs.oracle.com/en/java/javase/11/docs/api/java.compiler/javax/annotation/processing/Processor.html#process(java.util.Set,javax.annotation.processing.RoundEnvironment)
