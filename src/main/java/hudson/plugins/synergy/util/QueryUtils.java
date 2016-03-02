/*
 * © Copyright 2012 BG-Phoenics GmbH, Hannover
 * Alle Rechte sind vorbehalten.
 *
 * %created_by: u48jfe%
 * %date_created: 01.03.2016%
 * %version: 1 %
 *
 * history:
 * <Version> <TT.MM.JJJJ> <Kürzel> <Text>
 *      1     01.03.2016   u48jfe   Initialimplementierung.
 */
package hudson.plugins.synergy.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * @author u48jfe
 *
 */
public class QueryUtils {

  public static List<List<String>> createOptimizedSubLists(HashSet<String> l_hashSet, String maxQueryLength) {
    int parseInt = 512;
    try {
      parseInt = Integer.parseInt(maxQueryLength);
    } catch (NumberFormatException l_ex) {
    }
    List<List<String>> l_optimizedQueryObjects = new ArrayList<List<String>>();
    List<String> l_optList = new ArrayList<String>();
    int length = 0;
    final int maxLength = parseInt;
    for (String modifiedObject : l_hashSet) {
      if (maxLength > length + modifiedObject.length() + 4) {
        length += modifiedObject.length() + 4;
        l_optList.add(modifiedObject);
      } else {
        l_optimizedQueryObjects.add(l_optList);
        length = 0;
        l_optList = new ArrayList<String>();
        length += modifiedObject.length() + 4;
        l_optList.add(modifiedObject);
      }
    }
    l_optimizedQueryObjects.add(l_optList);
    return l_optimizedQueryObjects;
  }
}
