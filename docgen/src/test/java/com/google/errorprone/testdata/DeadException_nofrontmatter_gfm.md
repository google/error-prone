<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

# DeadException

__Exception created but not thrown__

<div style="float:right;"><table id="metadata">
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Tags</td><td>LikelyError</td></tr>
</table></div>

_Alternate names: ThrowableInstanceNeverThrown_

## The problem
The exception is created with new, but is not thrown, and the reference is lost.

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DeadException")` to the enclosing element.
