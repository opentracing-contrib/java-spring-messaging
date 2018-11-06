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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.integration.support.MutableMessageHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;


/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class MessageTextMapTest {

  @Test
  public void shouldGetIterator() {
    Map<String, String> headers = new HashMap<>(2);
    headers.put("h1", "v1");
    headers.put("h2", "v2");
    Message<String> message = MessageBuilder.withPayload("test")
        .copyHeaders(headers)
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);

    assertThat(map.iterator()).containsAll(headers.entrySet());
  }

  @Test
  public void shouldPutEntry() {
    Message<String> message = MessageBuilder.withPayload("test")
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);
    map.put("k1", "v1");

    assertThat(map.iterator()).contains(new AbstractMap.SimpleEntry<>("k1", "v1"));
  }

  @Test
  public void shouldGetMessageWithNewHeaders() {
    Message<String> message = MessageBuilder.withPayload("test")
        .build();
    MessageTextMap<String> map = new MessageTextMap<>(message);
    map.put("k1", "v1");
    Message<String> updatedMessage = map.getMessage();

    assertThat(updatedMessage.getPayload()).isEqualTo(message.getPayload());
    assertThat(updatedMessage.getHeaders()).contains(new AbstractMap.SimpleEntry<>("k1", "v1"));
  }

  @Test
  public void shouldPreserveTimestampAndId() {
    MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
    String id = UUID.randomUUID().toString();
    headers.put("id", id);
    headers.put("timestamp", "123456789");
    Message<String> message = MessageBuilder.createMessage("test", headers);

    MessageTextMap<String> map = new MessageTextMap<>(message);
    Message<String> copiedMessage = map.getMessage();

    assertThat(copiedMessage.getHeaders()).contains(new AbstractMap.SimpleEntry<>("timestamp", "123456789"));
    assertThat(copiedMessage.getHeaders()).contains(new AbstractMap.SimpleEntry<>("id", id));
  }

  @Test
  public void testPreserveType() {
    MutableMessageHeaders headers = new MutableMessageHeaders(new HashMap<>());
    headers.put("int", new Integer(1));
    headers.put("double", new Double(2.2));
    headers.put("string", "foo");

    MessageTextMap<String> textmap = new MessageTextMap<>(MessageBuilder.createMessage("test", headers));
    textmap.iterator();
    textmap.put("bar", "baz");

    Message<String> message = textmap.getMessage();
    Assert.assertEquals(new Double(2.2), message.getHeaders().get("double"));
    Assert.assertEquals(new Integer(1), message.getHeaders().get("int"));
    Assert.assertEquals("foo", message.getHeaders().get("string"));
    Assert.assertEquals("baz", message.getHeaders().get("bar"));
  }
}
