package com.bosonfields.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._

object FriendsRecommend {
  def friends(line: Array[String]): Array[(Int, Int)] = {
    
    if(line(0) == "" || line.length ==1){
        return Array.empty[(Int, Int)]
    }
    
    val user = line(0).toInt
    line(1).split(",").map(friend => (user, friend.toInt))   
  }
  
  def recommend_list(candidates: List[(Int, Int)], n: Int) : List[Int] = {
    candidates.sortBy(pairs => (-pairs._2, pairs._1)).take(n).map(pairs => pairs._1)
  }
  
  def main(args: Array[String]){
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    val sc = new SparkContext("local", "recommendFriends")
    
    val lines = sc.textFile("../cs550_data/data550_ass1.txt").cache()
    
    val user_friends = lines.map(line => line.split("\t"))
    
    //val get_no_friends = user_friends.filter(x => x.length ==1)
    //val no_friends_recom = get_no_friends.map(line => (line, Array.empty))
    
    val current_friends_pairs = user_friends.flatMap(line => friends(line))
    
    val friends_pairs = current_friends_pairs.join(current_friends_pairs)
    
    val del_repeat = friends_pairs.map(elem => elem._2).filter(elem => elem._1 !=elem._2)
    
    val del_current_friends = del_repeat.subtract(current_friends_pairs)
    
    val pair_value = del_current_friends.map(item => (item, 1)).reduceByKey(_ + _)
    
    val group_recommendation = pair_value.map(elem => (elem._1._1,(elem._1._2, elem._2))).groupByKey()
    
    val pickTopTen = group_recommendation.map(line => (line._1, recommend_list(line._2.toList, 10)))
    
    //val change_format = pickTopTen.map(list => list._1.toString + "\t" + list._2.map(x => x.toString).toArray.mkString(","))
    //val full_list = change_format.union(no_friends_recom)
    
    val print_array = sc.parallelize(Array("924", "8941", "8942", "9019", "9020", "9021", "9022", "9990", "9992", "9993"))
    
    val select_values = print_array.map(x => (x, x))
    
    val temp_select = pickTopTen.map(list => (list._1.toString, list._2.map(x =>x.toString).toArray))
    
    val specific_value = select_values.join(temp_select)
    
    val result = specific_value.map(line => (line._1.toInt, line._2)).sortByKey()
    
    val print_val = result.map(x => x._2).map(x => x._1 + "\t" + x._2.mkString(","))
    
    print_val.foreach(println)
  }
}