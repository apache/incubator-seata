package io.seata.console.consumer.kafka;

import io.seata.console.consumer.ConsumerInterface;

public class KafkaConsumer implements ConsumerInterface {
//    public static final String brokerList="";
//    public static final String topic="topic-demo";
//    public static final String groupId="group.demo";
//
//    public static final AtomicBoolean isRunning=new AtomicBoolean(true);
//
//    public static void main(String[] args){
//
//        //配置消费者客户端参数
//        Properties properties=initConfig();
//
//        //创建相应的消费者实例
//        KafkaConsumer<String,String> consumer=new KafkaConsumer<>(properties);
//
//        //订阅主题
//        consumer.subscribe(Arrays.asList(topic));
//
//        try {
//            //拉取消息并消费
//            while(isRunning.get()){
//
//                ConsumerRecords<String,String> records=consumer.poll(Duration.ofMillis(1000));
//
//                for (ConsumerRecord<String,String> record:records){
//
//                    System.out.println("topic="+record.topic()+",partition="+record.partition()+",offset="+record.offset());
//                    System.out.println("key="+record.key()+",value="+record.value());
//                    //do something to processor record.
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }finally {
//            consumer.close();
//        }
//    }
//
//
//    /**
//     * 配置消费者客户端参数
//     * */
//    private static Properties initConfig() {
//        Properties properties=new Properties();
//
////        properties.put("key.deserializer","org.apache.kafka.common.serialization.Deserializer");
//        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, Deserializer.class.getName());
//
////        properties.put("value.deserializer","org.apache.kafka.common.serialization.Deserializer");
//        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,Deserializer.class.getName());
//
//        //指定连接Kafka集群所需的broker地址清单,中间用逗号隔开,默认值为""
////        properties.put("bootstrap.server",brokerList);
//        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,brokerList);
//
//        //消费者所属的消费组的名称,默认值为""
////        properties.put("group.id",groupId);
//        properties.put(ConsumerConfig.GROUP_ID_CONFIG,groupId);
//
////        properties.put("client.id","consumer.client.id.demo");
//        //指定消费者客户端Id,如果不设置,则自动生成consumer-1,consumer-2
//        properties.put(ConsumerConfig.CLIENT_ID_CONFIG,"consumer.client.id.demo");
//
//        return properties;
//    }
@Override
public void consume() {

}
}
