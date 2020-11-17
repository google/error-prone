/*
 * Copyright 2017 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static java.util.stream.Collectors.toCollection;

import com.google.common.collect.ImmutableSet;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeKind;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
    name = "HidingField",
    summary = "Hiding fields of superclasses may cause confusion and errors",
    severity = WARNING,
    altNames = {"hiding", "OvershadowingSubclassFields"})
public class HidingField extends BugChecker implements ClassTreeMatcher {
  // List of types that are allowed to hide superclass fields
  private static final ImmutableSet<String> IGNORED_CLASSES =
      ImmutableSet.of("com.google.common.GoogleLogger", "java.util.logging.Logger");

  @Override
  public Description matchClass(ClassTree classTree, VisitorState visitorState) {
    List<VariableTree> originalClassMembers =
        classTree.getMembers().stream()
            .filter(mem -> mem instanceof VariableTree)
            .map(mem -> (VariableTree) mem)
            .filter(
                mem ->
                    !isSuppressed(ASTHelpers.getSymbol(mem))
                        && !isIgnoredType(mem)
                        && !isStatic(mem))
            .collect(toCollection(ArrayList::new));

    ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);

    while (!classSymbol.getSuperclass().getKind().equals(TypeKind.NONE)) {
      TypeSymbol parentSymbol = classSymbol.getSuperclass().asElement();
      List<Symbol> parentElements = parentSymbol.getEnclosedElements();

      Map<Name, VarSymbol> parentMembers =
          parentElements.stream()
              .filter(mem -> (mem instanceof VarSymbol))
              .map(mem -> (VarSymbol) mem)
              .filter(mem -> (!mem.isPrivate() && !mem.getModifiers().contains(Modifier.STATIC)))
              .collect(Collectors.toMap(Symbol::getSimpleName, mem -> mem));

      checkForHiddenFields(
          originalClassMembers,
          parentMembers,
          parentSymbol.getSimpleName(),
          classTree,
          visitorState);

      classSymbol = (ClassSymbol) parentSymbol;
    }

    return Description.NO_MATCH;
  }

  private void checkForHiddenFields(
      List<VariableTree> originalClassMembers,
      Map<Name, VarSymbol> parentMembers,
      Name parentClassName,
      ClassTree classTree,
      VisitorState visitorState) {
    Iterator<VariableTree> origVariableIterator = originalClassMembers.iterator();
    VariableTree origVariable = null;

    while (origVariableIterator.hasNext()) {
      origVariable = origVariableIterator.next();

      if (parentMembers.containsKey(origVariable.getName())) {
        if (isPackagePrivateAndInDiffPackage(
            parentMembers.get(origVariable.getName()), classTree)) {
          continue;
        }

        Description.Builder matchDesc = buildDescription(origVariable);

        matchDesc.setMessage(
            "Hiding fields of superclasses may cause confusion and errors. "
                + "This field is hiding a field of the same name in superclass: "
                + parentClassName);

        visitorState.reportMatch(matchDesc.build());
        origVariableIterator.remove();
      }
    }
  }

  private static boolean isIgnoredType(VariableTree variableTree) {
    VarSymbol varSymbol = ASTHelpers.getSymbol(variableTree);

    if (varSymbol != null) { // varSymbol is null when variable is primitive type
      return IGNORED_CLASSES.contains(varSymbol.getQualifiedName().toString());
    }

    return false;
  }

  private static boolean isStatic(VariableTree varTree) {
    return varTree.getModifiers().getFlags().contains(Modifier.STATIC);
  }

  private static boolean isPackagePrivateAndInDiffPackage(
      VarSymbol parentVariable, ClassTree currClass) {
    if (!parentVariable.getModifiers().contains(Modifier.PRIVATE)
        && !parentVariable.getModifiers().contains(Modifier.PROTECTED)
        && !parentVariable.getModifiers().contains(Modifier.PUBLIC)) { // package-private variable

      if (!parentVariable.packge().equals(ASTHelpers.getSymbol(currClass).packge())) {
        return true;
      }
    }
    return false;
  }
}
