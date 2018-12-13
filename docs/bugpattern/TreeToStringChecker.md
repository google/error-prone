For emitting fixes or formatting diagnostics `Tree.toString` should almost
always be avoided, since it pretty-prints the AST node and loses information
about whitespace, comments, and some constructs that are desugared early.

`VisitorState#getSourceForNode` is often a better choice.
