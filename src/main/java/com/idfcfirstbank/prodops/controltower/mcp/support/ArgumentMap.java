package com.idfcfirstbank.prodops.controltower.mcp.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ArgumentMap {

  private ArgumentMap() {}

  public static Map<String, Object> of(Object... keyValues) {
    if (keyValues.length % 2 != 0) {
      throw new IllegalArgumentException("Argument map requires an even number of values.");
    }
    Map<String, Object> values = new LinkedHashMap<>();
    for (int index = 0; index < keyValues.length; index += 2) {
      values.put(String.valueOf(keyValues[index]), keyValues[index + 1]);
    }
    return Collections.unmodifiableMap(values);
  }
}
