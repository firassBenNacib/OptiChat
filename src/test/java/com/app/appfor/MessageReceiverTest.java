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

        mockQueueService = mock(QueueService.class);
        mockMeterRegistry = mock(MeterRegistry.class);


        MessageReceiver messageReceiver = new MessageReceiver(mockMeterRegistry, mockQueueService);


        String message = "Test Message";
        messageReceiver.receiveMessage(message);


    }
}
