/*
 * Copyright 2017 Google Inc. All Rights Reserved.
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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.ClassTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Description.Builder;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.lang.model.element.Name;

/**
 * @author sulku@google.com (Marsela Sulku)
 * @author mariasam@google.com (Maria Sam)
 */
@BugPattern(
  name = "OvershadowingSubclassFields",
  category = JDK,
  summary = "Overshadowing variables of superclass causes confusion and errors",
  severity = WARNING
)
public class OvershadowingSubclassFields extends BugChecker implements ClassTreeMatcher {

  @Override
  public Description matchClass(ClassTree classTree, VisitorState visitorState) {
    List<Name> originalClassMembers =
        classTree
            .getMembers()
            .stream()
            .filter(mem -> mem instanceof VariableTree)
            .map(mem -> ((VariableTree) mem).getName())
            .collect(Collectors.toList());

    ClassSymbol classSymbol = ASTHelpers.getSymbol(classTree);

    StringBuilder runningListOfOvershadowingVariables = new StringBuilder();

    while (!Objects.equals(classSymbol.getSuperclass(), Type.noType)) {
      TypeSymbol parentSymbol = classSymbol.getSuperclass().asElement();
      List<Symbol> parentElements = parentSymbol.getEnclosedElements();

      List<Name> parentMembers =
          parentElements
              .stream()
              .filter(mem -> (mem instanceof VarSymbol && !((VarSymbol) mem).isPrivate()))
              .map(mem -> mem.getSimpleName())
              .collect(Collectors.toList());

      String listOfSameVars =
          originalClassMembers
              .stream()
              .filter(parentMembers::contains)
              .collect(Collectors.joining(", "));

      if (!listOfSameVars.isEmpty()) {
        if (!runningListOfOvershadowingVariables.toString().isEmpty()) {
          runningListOfOvershadowingVariables.append(", ");
        }
        runningListOfOvershadowingVariables.append(listOfSameVars);
      }

      classSymbol = (ClassSymbol) parentSymbol;
    }

    if (!runningListOfOvershadowingVariables.toString().isEmpty()) {
      Builder desc = buildDescription(classTree);

      desc.setMessage(
          "Overshadowing variables of superclass causes confusion and errors. "
              + "The following variables are in danger of overshadowing: "
              + runningListOfOvershadowingVariables.toString());

      return desc.build();
    }

    return Description.NO_MATCH;
  }
}
