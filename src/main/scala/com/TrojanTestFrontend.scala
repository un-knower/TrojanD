package com

import org.drools.KnowledgeBase
import org.drools.KnowledgeBaseFactory
import org.drools.builder.KnowledgeBuilder
import org.drools.builder.KnowledgeBuilderError
import org.drools.builder.KnowledgeBuilderErrors
import org.drools.builder.KnowledgeBuilderFactory
import org.drools.builder.ResourceType
import org.drools.logger.KnowledgeRuntimeLogger
import org.drools.logger.KnowledgeRuntimeLoggerFactory
import org.drools.io.ResourceFactory
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.event.KnowledgeRuntimeEventManager

import org.apache.spark._
import org.apache.spark.rdd.RDD

import org.drools.event._
import org.drools.runtime._
import org.drools.builder._

import com.model.other.Time
import com.model.Item
import com.model.other.Type
import com.model.problem.Trojan
import com.service.TroDItem

import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.kafka.common.serialization.StringDeserializer
import scala.util.matching.Regex

object TrojanTestFrontend {

  def main(args : Array[String]):Unit ={
    
    println("Run Trojan Test by Distributed Rule Engine")
    val sparkconf = new SparkConf().setAppName("TrojanDetectionTest")
    val sc = new SparkContext(sparkconf)
    val ssc = new StreamingContext(sc,Seconds(10))

    //Step1:Receive Data From Kafka

    //Used in kafka-0.8
    /*
    val zkQuorum = "master:2181" //Zookeeper服务器地址
    val group = "trojanD"  //Kafka Group Name
    val topic = "trojanD" //Kafka Topic Name
    val numThreads = 1
    val topicMap = topic.split(",").map((_,numThreads.toInt)).toMap
    val lineMap = KafkaUtils.createStream(ssc,zkQuorum,group,topicMap)
    val lines = lineMap.map(_._2)*/

    //Used in kafka-0.10
    val server:String = sys.env("KAFKA_SERVER")
    val topic:String = sys.env("KAFKA_TOPIC")
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> server,
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "trojanD",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )
    val topics = Array(topic)
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )
    val lines = stream.map(_.value)

    println("  [Debug] Begin Execution!")
    val pairRDD = lines.foreachRDD( dataRDD =>
      dataRDD.mapPartitions{ partition => {
        val newPartition = partition.map(p => {
          println("  [Debug] Begin Dealing the element: "+p);
          val item = new TroDItem("IP-hostmachine",p.split(" ")(0));
          item.setFromDataLine(p);
          item
        })
        println("  [Debug] Finish the data partition!");
        newPartition
        }
      }
      .foreachPartition( itemList => {
        var ksession:StatefulKnowledgeSession = GetKnowledgeSession()
        val time = new Time()
        ksession.insert(time)
        val ty   = new Type()
        ksession.insert(ty)
        itemList.foreach(item => ksession.insert(item))
        ksession.fireAllRules()
      })
    )

    ssc.start
    ssc.awaitTermination
  }

  def GetKnowledgeSession() : StatefulKnowledgeSession = {
    val config:KnowledgeBuilderConfiguration = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration()
    config.setProperty("drools.dialect.mvel.strict", "false")
    var kbuilder : KnowledgeBuilder  = KnowledgeBuilderFactory.newKnowledgeBuilder(config)
    val rules:String = sys.env("TROJAND_HOME")+"/rules/trojanD.drl"
    val logs:String = sys.env("TROJAND_HOME")+"/logs"
    kbuilder.add(ResourceFactory.newFileResource(rules), ResourceType.DRL)
    println("  [Debug]  Rule Error:"+kbuilder.getErrors().toString())
    var kbase : KnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase()
    kbase.addKnowledgePackages(kbuilder.getKnowledgePackages())
    var ksession : StatefulKnowledgeSession = kbase.newStatefulKnowledgeSession()
    var logger : KnowledgeRuntimeLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession,logs)
    ksession
  }
}

