# AMQP协议模型

![image-20240219092238930](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240219092238930.png)

- Server：又称为Broker，接受客户端的链接，实现AMQP实体服务

- Connection：连接，应用程序与Broker的网络连接

- channel：网络信道，几乎所有的操作都在channel中进行，是消息读写的通道，可建立多个channel，每个channel代表一个会话任务

- Message：消息本体，由Properties和Body组成，Proerties对消息进行装饰（优先级、延迟等特征），body是消息体内容

- Virtual host：虚拟地址，进行逻辑各级，一个VirtualHost里面可以有若干个Exchange和Queue，同一个VirtualHost不能有相同的Exchange或Queue

- Exchange：交换机，接受消息，根据路由键转发消息到绑定的队列

- Binding：Exchange和Queue之间的虚拟链接 binding中可以报刊rounting key

- Rounting key ：消息发送的路由规则

- Queue： 消息队列，保存并转发给消费者

  ## AMQP 消息流转

![image-20240219095003749](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240219095003749.png)

![image-20240219095038365](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240219095038365.png)

# 命令行与管理台

```
rabbitmqctl stop_app   	关闭应用
rabbitmqctl start_app  	启动应用
rabbitmqctl status		节点状态
rabbitmqctl add_user username password	添加用户
rabbitmqctl list_users					打印用户列表
rabbitmqctl delete_user username 删除用户
rabbitmqctl clear_permissions -p vhostpath username  用户权限清除
rabbitmqctl add_vhost vhostpath 创建虚拟主机
rabbitmqctl list_vhosts 打印虚拟主机列表
rabbitmqctl list_queues 打印队列信息
rabbitmqctl -p vhostpath purge_queue blue 清除队列里的消息
```

## Exchange 交换机

Exchange：接受消息，并根据路由键转发消息所绑定的队列

![image-20240219140615707](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240219140615707.png)

Exchange 交换机属性

- Name：交换机名称
- Type：类型 
  - direct 直连，发送到direct Exchange的消息被转发到RouteKey指定的Queue  exchange=doutingKey
  - Topic  发布\订阅，发送到Topic Exchange的消息会被转发到所有关心RouteKey中制定的Topic的Queue上
  - fanout、不处理路由键，简单的将队列板绑定到交换机上，发送到交换机上的消息都会被转发到该交换机绑定的队列上、fount是最快的
  - headers
- Durability：是否需要持久化
- Auto Delete：当最后一个绑定在Exchange上的队列删除后，自动删除Exchange
- Interal：当前exchange是否用于Rabbitmq内部使用，默认为FALSE
- Arguments：扩展参数，用户扩展AMQP协议参数

Binding-绑定

- Exchange和Exchange、queue之间绑定

# 消息可靠性投递 100%

## 生产端-可靠性投递

解决方案

- 消息落库、对消息状态达标
- 消息延迟投递、做二次确认，回掉检查

## 消费端-幂等性保证

解决方案：

- 数据库ID+指纹码
- 利用Redis原子性实现

## Confirm确认消息

消费者收到消息会给生产者一个应答

```
channel.addConfirmListener
```

## Return 消息机制







# RabbitMQ代码整合

## Spring-AMQP

- RabbitAdmin -- Exchange\Queue\Binding 之间关系声明

- SpringAMQP 声明

- RabbitTemplate

- SimpleMessageListenerContainer

- MessageListenerAdapter

- MessageConverter

  ## RabbitAdmin

  ```java
  package com.study.rabbitmq.config;
  
  import org.springframework.amqp.core.Binding;
  import org.springframework.amqp.core.BindingBuilder;
  import org.springframework.amqp.core.Queue;
  import org.springframework.amqp.core.TopicExchange;
  import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
  import org.springframework.amqp.rabbit.connection.ConnectionFactory;
  import org.springframework.amqp.rabbit.core.RabbitAdmin;
  import org.springframework.context.annotation.Bean;
  import org.springframework.stereotype.Component;
  
  @Component
  public class RabbitMQConfig {
  
      @Bean
      public ConnectionFactory connectionFactory(){
          CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
          connectionFactory.setAddresses("192.168.223.128:5672");
          connectionFactory.setUsername("guest");
          connectionFactory.setPassword("guest");
          connectionFactory.setVirtualHost("/");
          return connectionFactory;
      }
  
      @Bean
      public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory){
          RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);
          // setAutoStartup  必须设置为true 否则Spring不会加载RabbitAdmin
          rabbitAdmin.setAutoStartup(true);
          return rabbitAdmin;
      }
  
  
      /**
       * 针对消费者配置
       * 1. 设置交换机类型
       * 2. 将队列绑定到交换机
       FanoutExchange: 将消息分发到所有的绑定队列，无routingkey的概念
       HeadersExchange ：通过添加属性key-value匹配
       DirectExchange:按照routingkey分发到指定队列
       TopicExchange:多关键字匹配
       */
      @Bean
      public TopicExchange exchange001() {
          return new TopicExchange("topic001", true, false);
      }
  
      @Bean
      public Queue queue001() {
          return new Queue("queue001", true); //队列持久
      }
  
      @Bean
      public Binding binding001() {
          return BindingBuilder.bind(queue001()).to(exchange001()).with("spring.*");
      }
  }
  
  ```

  ## RabbitTemplate

  ![image-20240220113610660](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240220113610660.png)

```java
   @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        return rabbitTemplate;
    }
    
        @Test
    public void testSendMessage() throws Exception {
        //1 创建消息
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.getHeaders().put("desc", "信息描述..");
        messageProperties.getHeaders().put("type", "自定义消息类型..");
        Message message = new Message("Hello RabbitMQ".getBytes(), messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.amqp", message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                System.err.println("------添加额外的设置---------");
                message.getMessageProperties().getHeaders().put("desc", "额外修改的信息描述");
                message.getMessageProperties().getHeaders().put("attr", "额外新加的属性");
                return message;
            }
        });
    }

```

## SimpleMessageListenerContainer

- SimpleMessageListenerContainer可以进行动态监听消息，设置事务、事务管理、事务属性、设置消费者数量，进行批量消费,设置消息确认模式、自动确认，是否重回队列、异常捕获header函数，设置消费者标签生成策略，是否独占模式，消费者属性，设置具体的监听器、消息转换器

```java
@Bean
    public SimpleMessageListenerContainer messageListenerContainer(ConnectionFactory connectionFactory){
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        // 容器添加监听队列
        container.setQueues(queue001());
        // 设置监听数据量
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(5);
        // 是否重回队列 - FALSE
        container.setDefaultRequeueRejected(false);
        // 签收 - auto 自动签收
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //
        container.setExposeListenerChannel(true);
        // 设置客户端tag
        container.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {
                return queue + "_" + UUID.randomUUID().toString();
            }
        });
        // 消息监听处理
        container.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                System.out.println("---- 处理消费者"+new String(message.getBody()));
            }
        });
        return container;
    }
```

## 消息监听MessageListenerAdapter 以及 消息封装处理 MessageConverter 

``` java
// 消息坚定
MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
adapter.setDefaultListenerMethod("consumeMessage"); // 设置监听处理方法名
adapter.setMessageConverter(new TextMessageConverter()); // 设置消息封装类
container.setMessageListener(adapter);

// TextMessageConverter 
public class TextMessageConverter implements MessageConverter {
    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        return new Message(object.toString().getBytes(), messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        String contentType = message.getMessageProperties().getContentType();
        if(null != contentType && contentType.contains("text")) {
            return new String(message.getBody());
        }
        return message.getBody();
    }
}
// MessageDelegate 
public class MessageDelegate {
    public void handleMessage(byte[] messageBody) {
        System.err.println("默认方法, 消息内容:" + new String(messageBody));
    }

    public void consumeMessage(byte[] messageBody) {
        System.err.println("字节数组方法, 消息内容:" + new String(messageBody));
    }

    public void consumeMessage(String messageBody) {
        System.err.println("字符串方法, 消息内容:" + messageBody);
    }

    public void method1(String messageBody) {
        System.err.println("method1 收到消息内容:" + new String(messageBody));
    }

    public void method2(String messageBody) {
        System.err.println("method2 收到消息内容:" + new String(messageBody));
    }


    public void consumeMessage(Map messageBody) {
        System.err.println("map方法, 消息内容:" + messageBody);
    }


    public void consumeMessage(Order order) {
        System.err.println("order对象, 消息内容, id: " + order.getId() +
                ", name: " + order.getName() +
                ", content: "+ order.getContent());
    }

    public void consumeMessage(File file) {
        System.err.println("文件对象 方法, 消息内容:" + file.getName());
    }
}



```



# SpringBoot整合RabbitMQ

```JAVA
// 生产者
@Component
public class RabbitSender {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    //回调函数: confirm确认
    final RabbitTemplate.ConfirmCallback confirmCallback = new RabbitTemplate.ConfirmCallback() {
        @Override
        public void confirm(CorrelationData correlationData, boolean ack, String cause) {
            System.err.println("correlationData: " + correlationData);
            System.err.println("ack: " + ack);
            if(!ack){
                System.err.println("异常处理....");
            }
        }
    };

    //回调函数: return返回
    final RabbitTemplate.ReturnCallback returnCallback = new RabbitTemplate.ReturnCallback() {
        @Override
        public void returnedMessage(org.springframework.amqp.core.Message message, int replyCode, String replyText,
                                    String exchange, String routingKey) {
            System.err.println("return exchange: " + exchange + ", routingKey: "
                    + routingKey + ", replyCode: " + replyCode + ", replyText: " + replyText);
        }
    };


    //发送消息方法调用: 构建Message消息
    public void send(Object message, Map<String, Object> properties) throws Exception {
        MessageHeaders mhs = new MessageHeaders(properties);
        Message msg = MessageBuilder.createMessage(message, mhs);
        rabbitTemplate.setConfirmCallback(confirmCallback);
        rabbitTemplate.setReturnCallback(returnCallback);
        //id + 时间戳 全局唯一
        CorrelationData correlationData = new CorrelationData("1234567890");
        rabbitTemplate.convertAndSend("exchange-1", "springboot.abc", msg, correlationData);
    }

    //发送消息方法调用: 构建自定义对象消息
    public void sendOrder(Order order) throws Exception {
        rabbitTemplate.setConfirmCallback(confirmCallback);
        rabbitTemplate.setReturnCallback(returnCallback);
        //id + 时间戳 全局唯一
        CorrelationData correlationData = new CorrelationData("0987654321");
        rabbitTemplate.convertAndSend("exchange-2", "springboot.def", order, correlationData);
    }

}
```

```java
// 消费者
@Component
public class RabbitReceiver {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue-1",
                    durable="true"),
            exchange = @Exchange(value = "exchange-1",
                    durable="true",
                    type= "topic",
                    ignoreDeclarationExceptions = "true"),
            key = "springboot.*"
    )
    )
    @RabbitHandler
    public void onMessage(Message message, Channel channel) throws Exception {
        System.err.println("--------------------------------------");
        System.err.println("消费端Payload: " + message.getPayload());
        Long deliveryTag = (Long)message.getHeaders().get(AmqpHeaders.DELIVERY_TAG);
        //手工ACK
        channel.basicAck(deliveryTag, false);
    }


    /**
     *
     * @param order
     * @param channel
     * @param headers
     * @throws Exception
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${spring.rabbitmq.listener.order.queue.name}",
                    durable="${spring.rabbitmq.listener.order.queue.durable}"),
            exchange = @Exchange(value = "${spring.rabbitmq.listener.order.exchange.name}",
                    durable="${spring.rabbitmq.listener.order.exchange.durable}",
                    type= "${spring.rabbitmq.listener.order.exchange.type}",
                    ignoreDeclarationExceptions = "${spring.rabbitmq.listener.order.exchange.ignoreDeclarationExceptions}"),
            key = "${spring.rabbitmq.listener.order.key}"
    )
    )
    @RabbitHandler
    public void onOrderMessage(@Payload Order order,
                               Channel channel,
                               @Headers Map<String, Object> headers) throws Exception {
        System.err.println("--------------------------------------");
        System.err.println("消费端order: " + order.getId());
        Long deliveryTag = (Long)headers.get(AmqpHeaders.DELIVERY_TAG);
        //手工ACK
        channel.basicAck(deliveryTag, false);
    }
}
```

```yaml
# 配置文件
spring:
  rabbitmq:
    addresses: 192.168.11.76:5672
    username: guest
    password: guest
    virtual-host: /
    connection-timeout: 15000

    publisher-confirms: true # 生产者exchange确认
    publisher-returns: true # 生产者queue确认
    template:
      mandatory: true
    listener:
      simple:
        acknowledge-mode: manual
        concurrency: 5
        max-concurrency: 10
      order: # 自定义消费队列
        queue:
          name: queue-2
          durable: true
        exchange:
          name: exchange-2
          durable: true
          type: topic
          ignoreDeclarationExceptions: true
        key: springboot.*
```



![image-20240221125440948](C:\Users\zhangChi\AppData\Roaming\Typora\typora-user-images\image-20240221125440948.png)

# SpringCloudStream 整合Rabbitmq

```java
/**
 * 生产者
 * 这里的Barista接口是定义来作为后面类的参数，这一接口定义来通道类型和通道名称。
 * 通道名称是作为配置用，通道类型则决定了app会使用这一通道进行发送消息还是从中接收消息。
 */
public interface Barista {
    String OUTPUT_CHANNEL = "output_channel";

    //注解@Output声明了它是一个输出类型的通道，名字是output_channel。这一名字与app1中通道名一致，表明注入了一个名字为output_channel的通道，类型是output，发布的主题名为mydest。
    @Output(Barista.OUTPUT_CHANNEL)
    MessageChannel logoutput();

}


@EnableBinding(Barista.class)
@Service
public class RabbitmqSender {
    @Autowired
    private Barista barista;

    // 发送消息
    public String sendMessage(Object message, Map<String, Object> properties) throws Exception {
        try{
            MessageHeaders mhs = new MessageHeaders(properties);
            Message msg = MessageBuilder.createMessage(message, mhs);
            boolean sendStatus = barista.logoutput().send(msg);
            System.err.println("--------------sending -------------------");
            System.out.println("发送数据：" + message + ",sendStatus: " + sendStatus);
        }catch (Exception e){
            System.err.println("-------------error-------------");
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());

        }
        return null;
    }
}
```

``` java
/**
 * 消费者
 * 这里的Barista接口是定义来作为后面类的参数，这一接口定义来通道类型和通道名称。
 * 通道名称是作为配置用，通道类型则决定了app会使用这一通道进行发送消息还是从中接收消息。
 */
public interface ConsumerBarista {
    String INPUT_CHANNEL = "input_channel";

    //注解@Input声明了它是一个输入类型的通道，名字是Barista.INPUT_CHANNEL，也就是position3的input_channel。这一名字与上述配置app2的配置文件中position1应该一致，表明注入了一个名字叫做input_channel的通道，它的类型是input，订阅的主题是position2处声明的mydest这个主题
    @Input(ConsumerBarista.INPUT_CHANNEL)
    SubscribableChannel loginput();

}

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
```

``` yaml
# 服务配置
server:
  port: 8888
  context-path: /rabbitmq
# spring应用配置
spring:
  application:
    name: rabbitmq-cloud-stream
  # SpringCloud 服务配置
  cloud:
    stream:
      binders:
        output_channel:
          destination: exchange-3
          group: queue-3
          binder: rabbit_cluster
        input_channel:
          destination: exchange-3
          group: queue-3
          binder: rabbit_cluster
          consumer:
            concurrency: 1
      bindings:
        rabbit_cluster:
          type: rabbit
          environment:
            spring:
              rabbitmq:
                addresses: 192.168.11.76:5672
                username: guest
                password: guest
                virtual-host: /
        input_channel:
          consumer:
            requeue-rejected: false
            acknowledge-mode: MANUAL
            recovery-interval: 3000
            durable-subscription: true
            max-concurrency: 5
```









































