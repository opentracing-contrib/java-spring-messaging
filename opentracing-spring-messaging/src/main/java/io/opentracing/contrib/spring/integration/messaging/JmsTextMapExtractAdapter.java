/**
 * Copyright 2017-2019 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.spring.integration.messaging;

import io.opentracing.propagation.TextMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper class to extract span context from message properties
 */
public class JmsTextMapExtractAdapter implements TextMap {

  private final Map<String, String> map = new HashMap<>();
  static final String DASH = "_$dash$_";

  public JmsTextMapExtractAdapter(TextMap message) {
    if (message == null) {
      return;
    }
    Iterator it = message.iterator();
    while (it.hasNext()) {
      Map.Entry headerPair = (Map.Entry) it.next();
      String headerKey = (String) headerPair.getKey();
      Object headerValue = headerPair.getValue();
      if (headerValue instanceof String) {
        map.put(decodeDash(headerKey), (String) headerValue);
      }
    }
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    return map.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    throw new UnsupportedOperationException(
        "JmsTextMapExtractAdapter should only be used with Tracer.extract()");
  }

  private String decodeDash(String key) {
    return key.replace(DASH, "-");
  }
}