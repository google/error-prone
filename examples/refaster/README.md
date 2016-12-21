This is a Refaster template, allowing you to specify before-and-after versions
of java code.

Compile this with:

```shell
wget http://repo1.maven.org/maven2/com/google/errorprone/javac/9-dev-r3297-1/javac-9-dev-r3297-1.jar

java -cp javac-9-dev-r3297-1.jar:../../refaster/target/error_prone_refaster-2.0.16-SNAPSHOT.jar \
  com.google.errorprone.refaster.RefasterRuleCompiler \
  StringLengthToEmpty.java --out `pwd`/refactoring.out
```

This will compile the Refaster template and emit a serialized file that
represents that template into refactoring.out.

Then use the refactoring.out file in an Error Prone-based compile to refactor
according to this refactoring (the maven profile configured in this example
passes the correct arguments to the Error Prone compile):

```shell
cd ../maven/refaster-based-cleanup
mvn clean compile -Pfixerrors
```
