package com.awsproserv.kafkaandsparkstreaming

import kafka.producer.ProducerConfig
import java.util.Properties
import kafka.producer.Producer
import scala.util.Random
import kafka.producer.Producer
import kafka.producer.Producer
import kafka.producer.Producer
import kafka.producer.KeyedMessage
import java.util.Date

object ClickstreamKafkaProducer extends App {
  
  val events = args(0).toInt
  val topic = args(1)
  val brokers = args(2)
  
  val rnd = new Random()
  val props = new Properties()
  props.put("metadata.broker.list", brokers)
  props.put("serializer.class", "kafka.serializer.StringEncoder")
  props.put("producer.type", "async")
 
  val config = new ProducerConfig(props)
  val producer = new Producer[String, String](config)
  val t = System.currentTimeMillis()
  for (nEvents <- Range(0, events)) {
    val runtime = new Date().getTime();
    val ip = "192.168.2." + rnd.nextInt(255);
    val url = "www.amazon" + rnd.nextInt(255) + ".com";
    val msg = runtime + "," + nEvents + "," + url + "," + ip;
    val data = new KeyedMessage[String, String](topic, ip, msg);
    producer.send(data);
  }

  System.out.println("sent per second: " + events * 1000 / (System.currentTimeMillis() - t));
  producer.close();
  
}
