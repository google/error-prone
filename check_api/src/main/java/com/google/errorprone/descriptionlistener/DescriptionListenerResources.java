package com.google.errorprone.descriptionlistener;

import com.google.auto.value.AutoValue;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import javax.tools.JavaFileObject;

@AutoValue
public abstract class DescriptionListenerResources {
  public abstract Log getLog();
  public abstract JCCompilationUnit getCompilation();
  public abstract Context getContext();
  public abstract boolean getUseErrors();

  public static DescriptionListenerResources create(Log log, JCCompilationUnit compilation, Context context, boolean useErrors) {
    return new AutoValue_DescriptionListenerResources(log, compilation, context, useErrors);
  }
}
