package com.google.errorprone.apply;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Organizes imports according to hubspot's style guide.
 */
class HubSpotImportOrganizer implements ImportOrganizer {
  HubSpotImportOrganizer() {
  }

  private enum ImportGroup {
    STATIC,
    JAVA,
    JAVAX,
    ORG,
    COM,
    OTHER
  }

  @Override
  public OrganizedImports organizeImports(List<Import> imports) {
    OrganizedImports organized = new OrganizedImports();

    // Group into static and non-static.
    Multimap<ImportGroup, Import> importsByGroup = ArrayListMultimap.create();

    for (Import i : imports) {
      if (i.isStatic()) {
        importsByGroup.put(ImportGroup.STATIC, i);
      } else if (i.getType().startsWith("java.")) {
        importsByGroup.put(ImportGroup.JAVA, i);
      } else if (i.getType().startsWith("javax.")) {
        importsByGroup.put(ImportGroup.JAVAX, i);
      } else if (i.getType().startsWith("org.")) {
        importsByGroup.put(ImportGroup.ORG, i);
      } else if (i.getType().startsWith("com.")) {
        importsByGroup.put(ImportGroup.COM, i);
      } else {
        importsByGroup.put(ImportGroup.OTHER, i);
      }
    }
    Map<ImportGroup, List<Import>> sortedGroups = new HashMap<>();
    for (ImportGroup group : importsByGroup.keySet()) {
      List<Import> groupImports = new ArrayList<>(importsByGroup.get(group));
      groupImports.sort(Comparator.comparing(Import::getType));
      sortedGroups.put(group, groupImports);
    }
    // use the order of the enum to list our import blocks.
    organized.addGroups(sortedGroups, Arrays.asList(ImportGroup.values()));
    return organized;
  }
}
