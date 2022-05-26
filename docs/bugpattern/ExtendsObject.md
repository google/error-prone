`T extends Object` is redundant when using normal (non-Checker Framework
checked) code.

However, `T extends Object` compiles to the same bytecode as `T` when using
vanilla javac. So, when using Checker on vanilla javac's bytecode, `T extends
Object` does not imply non-null bounds *outside the same compilation unit*.
