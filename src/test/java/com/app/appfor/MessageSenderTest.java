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

		mockJmsTemplate = mock(JmsTemplate.class);


		MessageSender messageSender = new MessageSender(mockJmsTemplate);


		String message = "Test Message";
		messageSender.sendMessage(message);


		verify(mockJmsTemplate, times(1)).convertAndSend("message Queue", message);
	}
}
