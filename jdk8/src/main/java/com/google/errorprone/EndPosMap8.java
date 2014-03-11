package com.google.errorprone;

import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticPosition;
import com.sun.tools.javac.util.Position;

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

  /**
   * The JDK8 implementation of endPosMap returns NOPOS if there's no mapping for the given key.
   */
  @Override
  public Integer getEndPosition(DiagnosticPosition pos) {
    Integer result = pos.getEndPosition(table);
    return result != null ? result : Position.NOPOS;
  }

  @Override
  public Set<Map.Entry<JCTree, Integer>> entrySet() {
    return rawMap.entrySet();
  }

  /**
   * Adapt a Map<JCTree, Integer> into an EndPosTable, and return Position.NOPOS for keys that
   * aren't present.
   */
  private static class EndPosTableAdapter implements EndPosTable {

    private final Map<JCTree, Integer> map;

    public EndPosTableAdapter(Map<JCTree, Integer> map) {
      this.map = map;
    }

    @Override
    public int getEndPos(JCTree tree) {
      Integer result = map.get(tree);
      return result != null ? result : Position.NOPOS;
    }

    @Override
    public void storeEnd(JCTree tree, int endpos) {
      throw new IllegalStateException();
    }

    @Override
    public int replaceTree(JCTree oldtree, JCTree newtree) {
      throw new IllegalStateException();
    }
  }

  public static int getEndPos(DiagnosticPosition pos, Map<JCTree, Integer> map) {
    return pos.getEndPosition(new EndPosTableAdapter(map));
  }
}
