package com.study.rabbitmq.consumer;

import com.rabbitmq.client.Channel;
import com.study.rabbitmq.stream.ConsumerBarista;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@EnableBinding(ConsumerBarista.class)
@Service
public class RabbitmqReceiver {

    @StreamListener(ConsumerBarista.INPUT_CHANNEL)
    public void receiver(Message message) throws Exception {
        Channel channel = (com.rabbitmq.client.Channel) message.getHeaders().get(AmqpHeaders.CHANNEL);
        Long deliveryTag = (Long) message.getHeaders().get(AmqpHeaders.DELIVERY_TAG);
        System.out.println("Input Stream 1 接受数据：" + message);
        System.out.println("消费完毕------------");
        channel.basicAck(deliveryTag, false);
    }
}
