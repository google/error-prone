---
title: Error-Prone
layout: master
---

### It's common for even the best programmers to make simple mistakes. And commonly, a refactoring which seems safe can leave behind code which will never do what's intended.

We're used to getting help from the compiler, but it doesn't do much beyond static type checking. Using error-prone to augment the compiler's type analysis, you can catch more mistakes before they cost you time, or end up as bugs in production. We use error-prone in Google's Java build system to eliminate classes of serious bugs from entering our code, and we've open-sourced it, so you can too!

__error prone ...__

* __hooks into your standard build, so all developers run it without thinking__
* __tells you about mistakes immediately after they're made__
* __produces suggested fixes, allowing you to build tooling on it__

## How it works
__src/Main.java__
{% highlight java linenos %}
public class Main {
  public static void main(String[] args) {
    if (args.length < 1); //OOPS!
      throw new IllegalArgumentException("Missing required argument");
  }
}
{% endhighlight %}
{% highlight bash %}
$ ant
Buildfile: /Projects/error-prone/examples/ant/build.xml

compile:
    [javac] Compiling 1 source file to /Projects/error-prone/examples/ant/build
    [javac] src/Main.java:3: error: [EmptyIf] Empty statement after if
    [javac]     if (args.length < 1);
    [javac]     ^
    [javac]   did you mean 'if (args.length < 1)'?
    [javac] 1 error

BUILD FAILED
{% endhighlight %}