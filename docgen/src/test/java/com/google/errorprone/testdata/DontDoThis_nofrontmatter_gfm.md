<!--
*** AUTO-GENERATED, DO NOT MODIFY ***
To make changes, edit the @BugPattern annotation or the explanation in docs/bugpattern.
-->

# DontDoThis

__Don&#39;t do this; do List&lt;Foo&gt; instead__

<div style="float:right;"><table id="metadata">
<tr><td>Severity</td><td>ERROR</td></tr>
<tr><td>Tags</td><td>LikelyError</td></tr>
<tr><td>Provides Fix?</td><td>Yes, requires human attention</td></tr>
<div class=".more-info" data-qualified-name=com.google.errorprone.bugpatterns.DontDoThis></div>
</table></div>


## The problem
This is a bad idea, you want `List<Foo>` instead

## Suppression
Suppress false positives by adding the suppression annotation `@SuppressWarnings("DontDoThis")` to the enclosing element.
