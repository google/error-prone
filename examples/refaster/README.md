This is a Refaster template, allowing you to specify before-and-after versions
of java code.

Compile this with:

```shell
wget http://repo1.maven.org/maven2/com/google/errorprone/javac/9+181-r4173-1/javac-9+181-r4173-1.jar

javac \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED \
    -J--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED \
    -J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED \
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
