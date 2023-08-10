package com.app.appfor;

import com.app.appfor.Component.MessageReceiver;
import com.app.appfor.service.QueueService;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;

public class MessageReceiverTest {

    @Mock
    private QueueService mockQueueService;

    @Mock
    private MeterRegistry mockMeterRegistry;

    @Test
    public void testReceiveMessage() {
        // Create a mock QueueService and MeterRegistry
        mockQueueService = mock(QueueService.class);
        mockMeterRegistry = mock(MeterRegistry.class);

        // Create the MessageReceiver with the mock QueueService and MeterRegistry
        MessageReceiver messageReceiver = new MessageReceiver(mockMeterRegistry, mockQueueService);

        // Call the receiveMessage method
        String message = "Test Message";
        messageReceiver.receiveMessage(message);

        // Add assertions or verifications as needed for your specific scenario
    }
}
