`Tree#toString` shouldn't be used for Trees deriving from the code being
compiled, as it discards whitespace and comments.

This check only runs inside Error Prone code. Suggested replacements include:

*   Prefer `Elements#getConstantExpression` to `TreeMaker.Literal` for escaping
    constants in generated code.
*   `VisitorState#getSourceForNode` : it will give you the original source text.
    Note that for synthetic trees (e.g.: implicit constructors), that source may
    be `null`.
*   If the string representation was being used for comparison with keywords
    like `this` and `super`, try `tree.getName().contentEquals("this")`
*   One can also get the symbol name and use it for comparison :
    `ASTHelpers.getSymbol(tree).getSimpleName().toString()`
