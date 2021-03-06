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
import org.apache.spark.sql.{SQLContext, Row}

import org.drools.event._
import org.drools.runtime._
import org.drools.builder._

import com.model.other.Time
import com.model.Item
import com.model.other.Type
import com.model.problem.Trojan
import com.service.TroDItem

import scala.util.matching.Regex
import java.util.Properties

//This progrma is only used in test, DO NOT incude in the released version

object TrojanDTest {

  def main(args : Array[String]):Unit ={
    
    println("Run Trojan Test by Distributed Rule Engine")

    //Prepare Spark Environment
    val sparkconf = new SparkConf().setAppName("TrojanDetectionTest")
    val sc = new SparkContext(sparkconf)
    val sqlContext = new SQLContext(sc)
    sc.setLogLevel("ERROR")
    val prop = new Properties()
    prop.put("user", "root")
    prop.put("password", "123456")
    prop.put("driver","com.mysql.jdbc.Driver")    

    //Prepare Data
    val data = List("202.121.66.121 syn 35 104 up 104 7173 small 42 psh 199 199 down 199 15216 small 106 dns 2255 0 com 3 0 14","202.121.66.121 syn 35 104 up 104 7173 small 42 psh 199 199 down 199 15216 small 106 dns 2255 0 com 3 0 14","202.121.66.121 syn 35 104 up 104 7173 small 42 psh 199 199 down 199 15216 small 106 dns 2255 0 com 3 0 14")
    println("  [Debug] Begin Execution!")

    val pairRDD = sc.parallelize(data.map(p=>p.trim),1)
                    .mapPartitions{ partition => {
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
  }

  def GetKnowledgeSession() : StatefulKnowledgeSession = {
    val rules:String = sys.env("TROJAND_HOME")+"/rules/trojanD.drl"
    val logs:String = sys.env("TROJAND_HOME")+"/logs"
    val config:KnowledgeBuilderConfiguration = KnowledgeBuilderFactory.newKnowledgeBuilderConfiguration()
    config.setProperty("drools.dialect.mvel.strict", "false")
    var kbuilder : KnowledgeBuilder  = KnowledgeBuilderFactory.newKnowledgeBuilder(config)
    kbuilder.add(ResourceFactory.newFileResource(rules), ResourceType.DRL)
    println("  [Debug]  Rule Error:"+kbuilder.getErrors().toString())
    var kbase : KnowledgeBase = KnowledgeBaseFactory.newKnowledgeBase()
    kbase.addKnowledgePackages(kbuilder.getKnowledgePackages())
    var ksession : StatefulKnowledgeSession = kbase.newStatefulKnowledgeSession()
    var logger : KnowledgeRuntimeLogger = KnowledgeRuntimeLoggerFactory.newFileLogger(ksession,logs)
    ksession
  }
}

