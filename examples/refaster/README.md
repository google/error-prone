This is a Refaster template, allowing you to specify before-and-after versions
of java code.

Compile this with:

```shell
javac \
    -classpath error_prone_refaster-2.3.2-SNAPSHOT.jar \
    "-Xplugin:RefasterRuleCompiler --out `pwd`/refactoring.out" \
    StringLengthToEmpty.java
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
