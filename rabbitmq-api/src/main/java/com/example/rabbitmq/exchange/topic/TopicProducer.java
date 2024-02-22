package com.example.rabbitmq.exchange.topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TopicProducer {
    public static void main(String[] args) {

        try{
            ConnectionFactory connectionFactory = new ConnectionFactory() ;

            connectionFactory.setHost("192.168.223.128");
            connectionFactory.setPort(5672);
            connectionFactory.setVirtualHost("/");

            connectionFactory.setAutomaticRecoveryEnabled(true);
            connectionFactory.setNetworkRecoveryInterval(3000);
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();

            //4 声明
            String exchangeName = "test_topic";
            String routingKey1 = "user.save";
            String routingKey2 = "user.update";
            String routingKey3 = "user.delete.abc";

            //5 发送
            String msg = "Hello World RabbitMQ 4 Topic Exchange Message ...";
            channel.basicPublish(exchangeName, routingKey1 , null , msg.getBytes());
            channel.basicPublish(exchangeName, routingKey2 , null , msg.getBytes());
            channel.basicPublish(exchangeName, routingKey3 , null , msg.getBytes());
            channel.close();
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
