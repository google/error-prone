package com.google.errorprone;

import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

public class EndPosMap8 implements ErrorProneEndPosMap {

  /** Work around protected access restrictions on the EndPosTable implementations. */
  private static final class Util extends JavacParser {
    /** Don't instantiate this. */
    private Util() {
      super(null, null, false, false, false);
      throw new IllegalStateException();
    }

    public static boolean isSimpleEndPosTable(EndPosTable table) {
      return table instanceof JavacParser.SimpleEndPosTable;
    }

    public static boolean isEmptyEndPosTable(EndPosTable table) {
      return table instanceof JavacParser.EmptyEndPosTable;
    }

    /**
     * Use reflection to bypass access restrictions on SimpleEndPosTable.endPosMap.
     * This is pretty terrible, but we need the map's entrySet to construct WrappedTreeMaps.
     * TODO(cushon): investigate alternatives. Could we get we avoid the need to know the map's
     * contents by building the WrappedTreeMap lazily?
     */
    private static final Field END_POS_MAP_FIELD;
    static {
      try {
        END_POS_MAP_FIELD = JavacParser.SimpleEndPosTable.class.getDeclaredField("endPosMap");
        END_POS_MAP_FIELD.setAccessible(true);
      } catch (Exception e) {
        throw new LinkageError(e.getMessage());
      }
    }
    @SuppressWarnings("unchecked")
    public static Map<JCTree, Integer> getMap(EndPosTable table) {
      try {
        return (Map<JCTree, Integer>) END_POS_MAP_FIELD.get(table);
      } catch (Exception e) {
        throw new LinkageError(e.getMessage());
      }
    }
  }

  public static EndPosMap8 fromCompilationUnit(JCCompilationUnit compilationUnit) {
    EndPosTable table = compilationUnit.endPositions;

    if (Util.isEmptyEndPosTable(table)) {
      return null;
    }

    if (Util.isSimpleEndPosTable(table)) {
      return new EndPosMap8(table, Util.getMap(table));
    }

    throw new IllegalStateException();
  }

  private EndPosTable table;
  private Map<JCTree, Integer> rawMap;

  private EndPosMap8(EndPosTable table, Map<JCTree, Integer> rawMap) {
    this.table = table;
    this.rawMap = rawMap;
  }

  @Override
  public int getEndPosition(DiagnosticPosition pos) {
    return pos.getEndPosition(table);
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return rawMap.entrySet();
  }
}
