## Contributing

Want to contribute? Great! First, read this page (including the small print at the end).

### Before you contribute
Before we can use your code, you must sign the
[Google Individual Contributor License Agreement](https://developers.google.com/open-source/cla/individual?csw=1)
(CLA), which you can do online. The CLA is necessary mainly because you own the
copyright to your changes, even after your contribution becomes part of our
codebase, so we need your permission to use and distribute your code. We also
need to be sure of various other things—for instance that you'll tell us if you
know that your code infringes on other people's patents. You don't have to sign
the CLA until after you've submitted your code for review and a member has
approved it, but you must do it before we can put your code into our codebase.
Before you start working on a larger contribution, you should get in touch with
us first through the issue tracker with your idea so that we can help out and
possibly guide you. Coordinating up front makes it much easier to avoid
frustration later on.

### Code reviews
All submissions, including submissions by project members, require review. We
use Github pull requests for this purpose.

### Reporting issues

Error Prone checks are designed to catch common mistakes in real-world Java code
at compile time (see
[criteria for new checks](https://errorprone.info/docs/criteria#false-positives-false-negatives-and-bug-reports)).
They do not aim to be 100% sound (catching every possible way a bug could be
written) or to have a zero percent false positive rate on arbitrary code.

When evaluating bug reports against existing checks:

*   **False positives** are prioritized when they affect idiomatic, real-world
    code, especially for `ERROR`-severity checks.
*   **False negatives** are evaluated based on how likely the missed pattern is
    to occur in real-world code, and whether handling it would add undue
    complexity, compile-time cost, or false-positive risk to the check.
*   **Synthetic or automated reports**: Reports of false negatives (or false
    positives) constructed by mutating existing test cases into contrived or
    unidiomatic code patterns (for example, via automated/metamorphic testing or
    AI agents), especially for `WARNING`- or `SUGGESTION`-severity checks, are
    unlikely to be prioritized and may be closed without action. If you are
    using automated tools to find gaps in checks, please verify that the pattern
    actually occurs in real-world codebases before filing an issue, and avoid
    filing multiple issues for the same underlying pattern across different
    checks.

### The small print
Contributions made by corporations are covered by a different agreement than
the one above, the Software Grant and Corporate Contributor License Agreement.
