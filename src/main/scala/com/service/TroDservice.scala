package com.service

import com.convert.Convertor
import com.model.Item
import com.model.other.Time
import com.model.other.Request
import scala.collection.JavaConverters._
import scala.io.Source
import com.rpc.Client

import scala.collection.JavaConverters._

class TroDItem(itemname:String,itemobj:String) extends Item(itemname:String,itemobj:String){

  var client:Client = new Client() 

  //TrojanD service
  def SA_KMEANS_JUDGEMENT(time:Time,dnames:List[String]){
     clearVector()
     dnames.foreach( dname => dimensions.foreach( p => if (p.name==dname){vector.addValue(p.value)}))
     var request:Request = new Request()
     request.setTime(time)
     request.setMLParameter(vector)
     request.setRequestType("ML")
     request.setRequestMode("DEFAULT")
     //request.insertProblem("Detect Trojan in KMeans Model")
     var external = new Convertor("SA_KMEANS_JUDGEMENT",request)
     var internal:List[Request] = external.getInterface()
     client.send(internal.asJava)
  }
     
  def SA_KMEANS_JUDGEMENT(time:Time,dnames_java:java.util.List[String]){
     clearVector()
     val dnames:List[String] = dnames_java.asScala.toList    
     var strvector = ""
     dnames.foreach( dname => strvector += ( dname + " ") )
     println("  [KMEANS] vector requirment: " + strvector)
     var strdimen = ""
     dimensions.foreach( dname => strdimen += ( dname.name + " ") )
     println("  [KMEANS] dimension list: " + strdimen)
     for (dname <- dnames){
       for (dimension <- dimensions){
         if (dimension.name == dname) vector.addValue(dimension.value.toDouble)
       }
     }
     var request:Request = new Request()
     request.setTime(time)
     request.setMLParameter(vector)
     request.setRequestType("ML")
     request.setRequestMode("DEFAULT")
     var external = new Convertor("SA_KMEANS_JUDGEMENT",request)
     var internal:List[Request] = external.getInterface()
     var replys:List[Request] = client.send(internal.asJava).asScala.toList
     //The KMEANS judgement would only send one request and receive one reply
     var reply:Request = replys.apply(0)
     if (reply.ml.last.problem != "") {insertProblem(reply.ml.last.problem); kmeansFlag = 1;}
     //printProblems()
  }

  def SA_GMM_JUDGEMENT(time:Time,dnames_java:java.util.List[String]){
     clearVector()
     val dnames:List[String] = dnames_java.asScala.toList
     var strvector = ""
     dnames.foreach( dname => strvector += ( dname + " ") )
     println("  [GMM] vector requirment: " + strvector)
     var strdimen = ""
     dimensions.foreach( dname => strdimen += ( dname.name + " ") )
     println("  [GMM] dimension list: " + strdimen)
     for (dname <- dnames){
       for (dimension <- dimensions){
         if (dimension.name == dname) vector.addValue(dimension.value.toDouble)
       }
     }
     var request:Request = new Request()
     request.setTime(time)
     request.setMLParameter(vector)
     request.setRequestType("ML")
     request.setRequestMode("DEFAULT")
     var external = new Convertor("SA_GMM_JUDGEMENT",request)
     var internal:List[Request] = external.getInterface()
     var replys:List[Request] = client.send(internal.asJava).asScala.toList
     //The GMM judgement would only send one request and receive one reply
     var reply:Request = replys.apply(0)
     if (reply.ml.last.problem != "") {insertProblem(reply.ml.last.problem); gmmFlag = 1;}
     //printProblems()
  }

  def SA_BISECT_JUDGEMENT(time:Time,dnames_java:java.util.List[String]){
     val dnames:List[String] = dnames_java.asScala.toList
     var strvector = ""
     dnames.foreach( dname => strvector += ( dname + " ") )
     println("  [GMM] vector requirment: " + strvector)
     var strdimen = ""
     dimensions.foreach( dname => strdimen += ( dname.name + " ") )
     println("  [GMM] dimension list: " + strdimen)
     for (dname <- dnames){
       for (dimension <- dimensions){
         if (dimension.name == dname) vector.addValue(dimension.value.toDouble)
       }
     }
     var request:Request = new Request()
     request.setTime(time)
     request.setMLParameter(vector)
     request.setRequestType("ML")
     request.setRequestMode("DEFAULT")
     var external = new Convertor("SA_BISECT_JUDGEMENT",request)
     var internal:List[Request] = external.getInterface()
     var replys:List[Request] = client.send(internal.asJava).asScala.toList
     //The BISECT judgement would only send one request and receive one reply
     var reply:Request = replys.apply(0)
     if (reply.ml.last.problem != "") {insertProblem(reply.ml.last.problem); bisectFlag = 1;}
     //printProblems()
  }

}
