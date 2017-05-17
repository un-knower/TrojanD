package com.execute

import com.model.other.Request
import com.internal.InternalTrigger
import org.apache.spark.SparkContext 
import org.apache.spark.SparkConf
import org.apache.spark.mllib.clustering.GaussianMixtureModel
import scala.io.Source
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.linalg.Vector

class GMM(sparkContext:SparkContext){

  var sc:SparkContext = sparkContext
  var helper:ProblemHelper = new ProblemHelper()
  var model:GaussianMixtureModel = GaussianMixtureModel.load(sc,"file:///usr/local/TrojanD/GaussianModel")
  var meanList:Array[Double] = Helper.loadMean()
  var devList:Array[Double] = Helper.loadDev()
  var problemList:Array[Int] = helper.loadProblems("MetaData/GaussianModel.txt")

  def print(){
    var meanStr = "  [GMM] Mean: "
    meanList.foreach( item => {meanStr = meanStr + item.toString + " "})
    var devStr = "  [GMM] Dev: "
    devList.foreach( item => {devStr = devStr + item.toString + " "})
    var problemStr = "  [GMM] Problem: "
    problemList.foreach( item => {problemStr = problemStr + item.toString + " "})
    println(meanStr)
    println(devStr)
    println(problemStr)
  }

  def predict(data:List[Double]):Boolean={
    var i:Int = 0
    data.foreach( item => {
      val ele:Double = (item - meanList(i))/devList(i)
      i = i + 1
      ele
    })
    val vector:Vector = Vectors.dense(data.toArray)
    val cluster:Int = model.predict(vector)
    if (problemList.contains(cluster)) {return true}
    return false
  }
}
