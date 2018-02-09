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

import static io.opentracing.contrib.spring.integration.messaging.OpenTracingChannelInterceptor.COMPONENT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentracing.References;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class OpenTracingChannelInterceptorTest {

  @Mock
  private Tracer mockTracer;

  @Mock
  private Tracer.SpanBuilder mockSpanBuilder;

  @Mock
  private ScopeManager mockScopeManager;

  @Mock
  private Scope mockScope;

  @Mock
  private Span mockSpan;

  @Mock
  private SpanContext mockSpanContext;

  @Mock
  private MessageChannel mockMessageChannel;

  @Mock
  private AbstractMessageChannel mockAbstractMessageChannel;

  private Message<String> simpleMessage;

  private OpenTracingChannelInterceptor interceptor;

  @Before
  public void before() {
    MockitoAnnotations.initMocks(this);
    when(mockTracer.buildSpan(anyString())).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.asChildOf(any(SpanContext.class))).thenReturn(mockSpanBuilder);
    when(mockSpanBuilder.startActive(true)).thenReturn(mockScope);
    when(mockTracer.scopeManager()).thenReturn(mockScopeManager);
    when(mockScope.span()).thenReturn(mockSpan);
    when(mockSpanBuilder.withTag(anyString(), anyString())).thenReturn(mockSpanBuilder);
    when(mockSpan.context()).thenReturn(mockSpanContext);

    interceptor = new OpenTracingChannelInterceptor(mockTracer);
    simpleMessage = MessageBuilder.withPayload("test")
        .build();
  }

  @Test
  public void preSendShouldGetNameFromGenericChannel() {
    interceptor.preSend(simpleMessage, mockMessageChannel);
    verify(mockTracer).buildSpan(String.format("send:%s", mockMessageChannel.toString()));
  }

  @Test
  public void preSendShouldGetNameFromAbstractMessageChannel() {
    interceptor.preSend(simpleMessage, mockAbstractMessageChannel);
    verify(mockAbstractMessageChannel, times(2)).getFullChannelName();
  }

  @Test
  public void preSendShouldStartSpanForClientSentMessage() {
    Message<?> message = interceptor.preSend(simpleMessage, mockMessageChannel);
    assertThat(message.getPayload()).isEqualTo(simpleMessage.getPayload());
    assertThat(message.getHeaders()).containsKey(Headers.MESSAGE_SENT_FROM_CLIENT);

    verify(mockTracer).buildSpan(String.format("send:%s", mockMessageChannel.toString()));
    verify(mockSpanBuilder).startActive(true);
    verify(mockSpanBuilder).withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME);
    verify(mockSpanBuilder).withTag(Tags.MESSAGE_BUS_DESTINATION.getKey(), mockMessageChannel.toString());
    verify(mockSpanBuilder).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_PRODUCER);
  }

  @Test
  public void preSendShouldStartSpanForServerReceivedMessage() {
    Message<?> originalMessage = MessageBuilder.fromMessage(simpleMessage)
        .setHeader(Headers.MESSAGE_SENT_FROM_CLIENT, true)
        .build();
    Message<?> message = interceptor.preSend(originalMessage, mockMessageChannel);
    assertThat(message.getPayload()).isEqualTo(originalMessage.getPayload());

    verify(mockTracer).extract(eq(Format.Builtin.TEXT_MAP), any(MessageTextMap.class));
    verify(mockTracer).buildSpan(String.format("receive:%s", mockMessageChannel.toString()));
    verify(mockSpanBuilder).addReference(References.FOLLOWS_FROM, null);
    verify(mockSpanBuilder).startActive(true);
    verify(mockSpanBuilder).withTag(Tags.COMPONENT.getKey(), COMPONENT_NAME);
    verify(mockSpanBuilder).withTag(Tags.MESSAGE_BUS_DESTINATION.getKey(), mockMessageChannel.toString());
    verify(mockSpanBuilder).withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CONSUMER);
  }

  @Test
  public void afterSendCompletionShouldDoNothingWithoutSpan() {
    interceptor.afterSendCompletion(null, null, true, null);

    verify(mockSpan, times(0)).log(anyString());
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForServerSendMessage() {
    Message<?> message = MessageBuilder.fromMessage(simpleMessage)
        .setHeader(Headers.MESSAGE_CONSUMED, true)
        .build();
    when(mockScopeManager.active()).thenReturn(mockScope);

    interceptor.afterSendCompletion(message, null, true, null);

    verify(mockScope).close();
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForClientSendMessage() {
    when(mockScopeManager.active()).thenReturn(mockScope);

    interceptor.afterSendCompletion(simpleMessage, null, true, null);

    verify(mockScope).close();
  }

  @Test
  public void afterSendCompletionShouldFinishSpanForException() {
    when(mockScopeManager.active()).thenReturn(mockScope);

    interceptor.afterSendCompletion(simpleMessage, null, true, new Exception("test"));

    verify(mockSpan).setTag(Tags.ERROR.getKey(), true);
    verify(mockScope).close();
  }

  @Test
  public void beforeHandleShouldOlyGetActiveSpan() {
    interceptor.beforeHandle(null, null, null);

    verify(mockTracer).activeSpan();
  }

  @Test
  public void afterMessageHandledShouldOnlyGetActiveSpan() {
    interceptor.afterMessageHandled(null, null, null, null);

    verify(mockTracer).activeSpan();
    verify(mockSpan, times(0)).setTag(anyString(), anyBoolean());
  }

  @Test
  public void afterMessageHandledShouldGetActiveSpanAndTagAnException() {
    when(mockTracer.activeSpan()).thenReturn(mockSpan);

    interceptor.afterMessageHandled(null, null, null, new Exception("test"));

    verify(mockTracer).activeSpan();
    verify(mockSpan).setTag(Tags.ERROR.getKey(), true);
  }

}
