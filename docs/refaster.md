---
title: Refaster templates
layout: documentation
---

In addition to [patching] your code using the checks built-in to Error Prone, we've developed a mechanism to refactor your code using before-and-after templates (we call them Refaster templates). Once you write these templates, you compile them into .refaster files, then use the Error Prone compiler to refactor your code according to those rules.

Refaster is described in more detail in a [research paper][lowasser-paper] presented by Louis Wasserman at the _Workshop for Refactoring Tools_.

## Building Refaster Templates

Explaining how to write refaster rules is best done by example:

```java
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.AlsoNegation;
import com.google.errorprone.refaster.annotation.BeforeTemplate;

public class StringIsEmpty {
  @BeforeTemplate
  boolean equalsEmptyString(String s) {
  	return s.equals("");
  }

  @BeforeTemplate
  boolean lengthEquals0(String string) {
    return string.length() == 0;
  }

  @AfterTemplate
  @AlsoNegation
  boolean optimizedMethod(String string) {
    return string.isEmpty();
  }
}
```

Refaster templates are any class with multiple methods with the same return type and list of arguments. One of the methods should be annotated `@AfterTemplate`, and every other method should be annotated with `@BeforeTemplate`. With this template, any code calling `String#equals` passing in the empty string literal, or calling `String#length` and comparing it to 0 will be replaced by a call to `String#isEmpty`. Notably, no matter how the String expression is generated, Refaster will do the replacement:

```
boolean b = someChained().methodCall().returningAString().length() == 0;

becomes

boolean b = someChained().methodCall().returningAString().isEmpty();


if (this.someStringField.equals(""))

becomes
 
if (this.someStringField.isEmpty())

```

There are other annotations in the [refaster.annotations] package that allow you to express more complex before-and-after refactorings, with examples about how to use them in their javadoc.

In the above example, `@AlsoNegation` is used to signal that the rule can also match the logical negation of the `@BeforeTemplate` bodies (`string.length() != 0` becomes `!string.isEmpty()`);

## Running the Refaster refactoring

TIP: These instructions are valid as of the most recent snapshot at HEAD, and are subject to change

Use the error prone javac jar and the error_prone_refaster jar to compile the refaster template:

```shell
wget http://repo1.maven.org/maven2/com/google/errorprone/javac/9-dev-r3297-1/javac-9-dev-r3297-1.jar
wget https://oss.sonatype.org/content/repositories/snapshots/com/google/errorprone/error_prone_refaster/2.0.16-SNAPSHOT/error_prone_refaster-2.0.16-20170121.005350-7.jar

java -cp javac-9-dev-r3297-1.jar:error_prone_refaster-2.0.16-20170121.005350-7.jar \
  com.google.errorprone.refaster.RefasterRuleCompiler \
  StringIsEmpty.java --out `pwd`/myrule.refaster
 ```

 You should see a file named `myrule.refaster` in your current directory. To use this to refactor your code, add the following flags to the Error Prone compiler (this is similar to [patching]):

 ```
-XepPatchChecks:refaster:/full/path/to/myrule.refaster
-XepPatchLocation:/full/path/to/your/source/root
```

This will generate a unified diff file named `error-prone.patch` that you can apply similarly to how you would apply other patches:

```shell
cd /full/path/to/your/source/root
patch -p0 -u -i error-prone.patch
```

[patching]: patching
[refaster.annotations]: ../api/latest/com/google/errorprone/refaster/annotation/package-frame.html
[lowasser-paper]: https://research.google.com/pubs/archive/41876.pdf