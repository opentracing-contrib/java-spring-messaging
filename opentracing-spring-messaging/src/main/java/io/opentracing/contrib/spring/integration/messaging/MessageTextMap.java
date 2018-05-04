/**
 * Copyright 2017-2018 The OpenTracing Authors
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
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageTextMap<T> implements TextMap {

  private final Message<T> message;

  private final MutableMessageHeaders headers;

  public MessageTextMap(Message<T> message) {
    this.message = message;
    this.headers = new MutableMessageHeaders(message.getHeaders());
  }

  @Override
  public Iterator<Map.Entry<String, String>> iterator() {
    Map<String, String> stringHeaders = new HashMap<>(headers.size());
    headers.forEach((k, v) -> stringHeaders.put(k, String.valueOf(v)));
    return stringHeaders.entrySet().iterator();
  }

  @Override
  public void put(String key, String value) {
    headers.put(key, value);
  }

  public Message<T> getMessage() {
    return MessageBuilder.fromMessage(message)
      .copyHeaders(headers)
      .build();
  }
}
