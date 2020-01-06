package com.google.errorprone.descriptionlistener;

import com.google.auto.value.AutoValue;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

import javax.tools.JavaFileObject;

@AutoValue
public abstract class DescriptionListenerResources {
  public abstract Log getLog();
  public abstract EndPosTable getEndPositions();
  public abstract JavaFileObject getSourceFile();
  public abstract Context getContext();
  public abstract boolean getUseErrors();

  public static DescriptionListenerResources create(Log log, EndPosTable endPosTable, JavaFileObject sourceFile, Context context, boolean useErrors) {
    return new AutoValue_DescriptionListenerResources(log, endPosTable, sourceFile, context, useErrors);
  }
}
