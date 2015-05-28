---
title: Criteria for new checks
layout: documentation
---

Error Prone gives us a powerful tool to disallow certain patterns from entering our Java code. We must be careful how we use it so that we benefit users without creating busywork for them. 

## Criteria for admitting new Severity.ERROR checks that are enabled by default

An Error Prone bug pattern (error) should have the following properties:

1. *The bug should be easy to understand.*  The problem should be obvious once the compiler points it out.  
1. *The fix should be easy to make.* For example, “Swap the order of these arguments,” or “Delete this semicolon,” not “Introduce a new subclass and override methods A, B, and C.”
1. *The bug pattern should have no false positives.*  In essentially no cases should the detected code actually be working as intended.
1. *The bug should represent a correctness issue.* We want to fix impactful bugs, not enforce best practices or style rules.  We shouldn’t nitpick a user’s code.
1. *The bug should occur with a small but noticeable frequency.*  There is no point in detecting bug patterns that never actually occur, but if a bug pattern occurs too frequently, it’s likely that it’s not causing any real problems.  We don’t want to overwhelm people with too many errors on apparently “correct” code.

The bottom line is that, when a user sees one of our errors, she should think, “I’m glad Error Prone caught that for me.”

### Examples

#### Positive examples

The PreconditionsCheckNotNull check flags uses of the
Preconditions.checkNotNull method with a literal as the first argument.  Since
the first argument is the variable to be checked for non-nullity, passing a
literal makes this a no-op.  The problem is clear (“You switched the order of
the arguments”), it is never what the programmer intended, it could cause a
real problem (a NullPointerException in a later part of the program), and it
occurs with reasonable frequency. 

The ArrayEquals check flags comparisons between two arrays that use the
Object.equals method.  The intent in these cases is to compare the content of
the arrays, but Object.equals compares for reference equality.  The problem is
clear once explained, in none of the cases did the programmer actually intend
to compare for reference equality, it is a correctness problem since the arrays
are not actually being compared, and it occurs occasionally.

#### Negative examples

Consider a check that flags uses of “new Integer(i)” and suggests using
“Integer.valueOf(i)” instead.  This improves performance and is correct for
most cases.  However, this bug is not a correctness problem, and in most cases
the performance impact is unimportant.  It also occurs frequently enough that
an error message is likely to annoy users.

Consider a check that detects that an @Override annotation is used on all
methods that override another method.  This is part of our style guide but is
not automatically enforced.  However, this is not really a correctness problem,
and my intuition is that it happens with pretty high frequency.  Making this an
error would be nitpicking.

## Criteria for admitting new Severity.WARNING checks

NOTE: These criteria are still under discussion. Feel free to email
error-prone-discuss@googlegroups.com if you have strong feelings on the topic.

Warnings can be style guide violations, potential serious bugs, or performance
gotchas. There are several reasons to have a warning instead of a compiler
error.

A warning should have the following properties:

1. *The warning should be easy to understand and the fix should be clear.*  The
problem should be obvious and actionable when pointed out.

2. *The warning should have very few false positives.* Developers should feel
that we are pointing out an actual issue at least 90% of the time.

3. *The warning should be for something that has the potential for significant
impact.* We want the warnings to be important enough so that when developers
see them they take them seriously and often choose to fix them.

4. *The warning should occur with a small but noticeable frequency.*  There is
no point in detecting warnings that never actually occur, but if a warning
occurs too frequently, it’s likely that it’s not causing any real problems.  We
don’t want to overwhelm people with too many warnings.

The bottom line is that, when a user sees one of our warnings, she should
think, “I’m glad the compiler pointed that out.”
