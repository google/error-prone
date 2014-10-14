---
title: How error-prone and Findbugs compare
layout: master
---

# Introduction

Before starting error-prone, we worked with Findbugs author Bill Pugh to run Findbugs on Google's source code as changes are made, to identify newly added bugs. Our experience with Findbugs was mixed, because of the number of existing occurrences of Findbugs issues, and the difficulty of building it into code review tools.

Major differences:

* Findbugs inspects compiled bytecode; error-prone inspects Java source files
* Findbugs includes warnings for things like bad practices; error-prone only reports errors

# Findbugs is better for

**Catalog of detectors**

Findbugs has a large number of detectors which have been built over time, and allows third-party plugins to contribute additional detectors.

**Non-critical warnings**

Findbugs results include a list of potential bad practices, as well as some false positives. If you have the inclination to inspect this list for things you care to fix, you can clean up your code.

**Integration with IDE's**

There are plugins for most IDEs which show Findbugs issues in real-time or at compile-time. These haven't yet been created for error-prone.

# error-prone is better for

**Preventing new occurrences**

Errors which are caught sooner are cheaper to fix.

By reporting errors in the Java compiler, error-prone gives the same user interface as syntax or type errors. This works well with existing tools, and is easy to enable in your build. This way, you prevent any occurrences from being added to your project.

With Findbugs, you can add tooling to your build, but it's difficult to uniformly enforce this for your whole project since not all issues reported must be fixed, and not all developers run the tool.

**Suggested fixes and refactoring**

Since we inspect source files and have the AST available, we can suggest a code modification to correct an error. This will be useful for IDE plugins. It also lets us apply a fix for all existing occurrences in a large codebase like Google's, which is a prerequisite to enabling the error.

**Ease of adding detector/checks**

Adding a detector to Findbugs is difficult, as it requires knowledge of Java bytecode. Since error-prone operates on the AST, the model is already familiar to most Java programmers.