package com.app.appfor;

import com.app.appfor.Component.MessageSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.jms.core.JmsTemplate;

import static org.mockito.Mockito.*;

public class MessageSenderTest {

	@Mock
	private JmsTemplate mockJmsTemplate;

	@Test
	public void testSendMessage() {
		// Create a mock JmsTemplate
		mockJmsTemplate = mock(JmsTemplate.class);

		// Create the MessageSender with the mock JmsTemplate
		MessageSender messageSender = new MessageSender(mockJmsTemplate);

		// Call the sendMessage method
		String message = "Test Message";
		messageSender.sendMessage(message);

		// Verify that the convertAndSend method was called with the correct arguments
		verify(mockJmsTemplate, times(1)).convertAndSend("message Queue", message);
	}
}
