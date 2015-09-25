/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.MessageProcessorPathResolver;
import org.mule.api.execution.LocationExecutionContextProvider;
import org.mule.api.processor.MessageProcessor;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.exception.MessagingExceptionLocationProvider;
import org.mule.tck.SerializationTestUtils;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;

import javax.xml.namespace.QName;

import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class MessagingExceptionTestCase extends AbstractMuleContextTestCase
{

    private static final String message = "a message";
    private static final String value = "Hello world!";
    
    private LocationExecutionContextProvider locationProvider = new MessagingExceptionLocationProvider();
    
    @Mock
    private MuleEvent mockEvent;

    @Before
    public void before()
    {
        MuleContext mockContext = mock(MuleContext.class);
        DefaultMuleConfiguration mockConfiguration = mock(DefaultMuleConfiguration.class);
        when(mockConfiguration.getId()).thenReturn("MessagingExceptionTestCase");
        when(mockContext.getConfiguration()).thenReturn(mockConfiguration);
        when(mockEvent.getMuleContext()).thenReturn(mockContext);
    }
    
    @Test
    public void getCauseExceptionWithoutCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat((MessagingException) exception.getCauseException(), is(exception));
    }

    @Test
    public void getCauseExceptionWithMuleCause()
    {
        DefaultMuleException causeException = new DefaultMuleException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void getCauseExceptionWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((DefaultMuleException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void getCauseExceptionWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((IOException) exception.getCauseException(), is(causeException));
    }

    @Test
    public void getCauseExceptionWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat((ConnectException) exception.getCauseException(), is(causeCauseException));
    }

    @Test
    public void causedByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedBy(Exception.class), Is.is(true));
        assertThat(exception.causedBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedBy(IOException.class), Is.is(false));
    }

    @Test
    public void causedByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
    }

    @Test
    public void causedByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(IOException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
        assertThat(exception.causedBy(Exception.class), is(true));
        assertThat(exception.causedBy(NullPointerException.class), is(false));
    }

    @Test
    public void causedByWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedBy(NullPointerException.class), is(false));
        assertThat(exception.causedBy(SocketException.class), is(true));
        assertThat(exception.causedBy(IOException.class), is(true));
        assertThat(exception.causedBy(MessagingException.class), is(true));
    }

    @Test
    public void causedExactlyByWithNullCause()
    {
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent);
        assertThat(exception.causedExactlyBy(MessagingException.class), Is.is(true));
        assertThat(exception.causedExactlyBy(Exception.class), Is.is(false));
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), Is.is(false));
        assertThat(exception.causedExactlyBy(IOException.class), Is.is(false));
    }

    @Test
    public void causedExactlyByWithMuleCauseWithMuleCause()
    {
        DefaultMuleException causeCauseException = new DefaultMuleException("");
        DefaultMuleException causeException = new DefaultMuleException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(DefaultMuleException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    }

    @Test
    public void causedExactlyByWithNonMuleCause()
    {
        IOException causeException = new IOException("");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(IOException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
        assertThat(exception.causedExactlyBy(Exception.class), is(false));
        assertThat(exception.causedExactlyBy(NullPointerException.class), is(false));
    }

    @Test
    public void causedExactlyByWithNonMuleCauseWithNonMuleCause()
    {
        ConnectException causeCauseException = new ConnectException();
        IOException causeException = new IOException(causeCauseException);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""),mockEvent,causeException);
        assertThat(exception.causedExactlyBy(ConnectException.class), is(true));
        assertThat(exception.causedExactlyBy(SocketException.class), is(false));
        assertThat(exception.causedExactlyBy(IOException.class), is(true));
        assertThat(exception.causedExactlyBy(MessagingException.class), is(true));
    }
    
    @Test
    public void withFailingProcessorNoPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        when(mockEvent.getFlowConstruct()).thenReturn(null);
        when(mockProcessor.toString()).thenReturn("Mock@1");
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase"));
    }

    @Test
    public void withFailingProcessorPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        MessageProcessorPathResolver pathResolver = mock(MessageProcessorPathResolver.class, withSettings().extraInterfaces(FlowConstruct.class));
        when(pathResolver.getProcessorPath(eq(mockProcessor))).thenReturn("/flow/processor");
        when(mockEvent.getFlowConstruct()).thenReturn((FlowConstruct) pathResolver);
        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/flow/processor @ MessagingExceptionTestCase"));
    }

    @Test
    public void withFailingProcessorNotPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class);
        FlowConstruct nonPathResolver = mock(FlowConstruct.class);
        when(mockEvent.getFlowConstruct()).thenReturn(nonPathResolver);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase"));
    }

    private static QName docNameAttrName = new QName("http://www.mulesoft.org/schema/mule/documentation", "name");

    @Test
    public void withAnnotatedFailingProcessorNoPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        when(mockEvent.getFlowConstruct()).thenReturn(null);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase (Mock Component)"));
    }

    @Test
    public void withAnnotatedFailingProcessorPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        MessageProcessorPathResolver pathResolver = mock(MessageProcessorPathResolver.class, withSettings().extraInterfaces(FlowConstruct.class));
        when(pathResolver.getProcessorPath(eq(mockProcessor))).thenReturn("/flow/processor");
        when(mockEvent.getFlowConstruct()).thenReturn((FlowConstruct) pathResolver);

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("/flow/processor @ MessagingExceptionTestCase (Mock Component)"));
    }

    @Test
    public void withAnnotatedFailingProcessorNotPathResolver()
    {
        MessageProcessor mockProcessor = mock(MessageProcessor.class, withSettings().extraInterfaces(AnnotatedObject.class));
        when(((AnnotatedObject) mockProcessor).getAnnotation(eq(docNameAttrName))).thenReturn("Mock Component");
        FlowConstruct nonPathResolver = mock(FlowConstruct.class);
        when(mockEvent.getFlowConstruct()).thenReturn(nonPathResolver);
        when(mockProcessor.toString()).thenReturn("Mock@1");

        MessagingException exception = new MessagingException(CoreMessages.createStaticMessage(""), mockEvent, mockProcessor);
        exception.getInfo().putAll(locationProvider.getContextInfo(mockEvent, mockProcessor));
        assertThat(exception.getInfo().get(LocatedMuleException.INFO_LOCATION_KEY).toString(), is("Mock@1 @ MessagingExceptionTestCase (Mock Component)"));
    }

    @Test
    public void serializableMessagingException() throws Exception
    {
        TestSerializableMessageProcessor processor = new TestSerializableMessageProcessor();
        processor.setValue(value);

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertThat(e.getMessage(), containsString(message));
        assertThat(e.getFailingMessageProcessor(), not(nullValue()));
        assertThat(e.getFailingMessageProcessor(), instanceOf(TestSerializableMessageProcessor.class));
        assertThat(((TestSerializableMessageProcessor) e.getFailingMessageProcessor()).getValue(), is(value));
    }

    @Test
    public void nonSerializableMessagingException() throws Exception
    {
        TestNotSerializableMessageProcessor processor = new TestNotSerializableMessageProcessor();

        MessagingException e = new MessagingException(MessageFactory.createStaticMessage(message),
            getTestEvent(""), processor);

        e = SerializationTestUtils.testException(e, muleContext);

        assertThat(e.getMessage(), containsString(message));
        assertThat(e.getFailingMessageProcessor(), is(nullValue()));
    }
}
