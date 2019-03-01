package com.bosonfields.spark

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.log4j._

object ProductRecommendation {
  def main(args: Array[String]){
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    val sc = new SparkContext("local", "productrecommend")
    
    val lines = sc.textFile("../cs550_data/browsing.txt").cache()
    
    val items_inline = lines.map(line =>line.split(" "))
    
    val items = lines.flatMap(line => line.split(" "))
    
    val item_count = items.map(elem => (elem, 1)).reduceByKey(_ + _).filter(line => line._2 >=100)
    
    val candidate_items = item_count.map(keyValue => keyValue._1).collect()
    
    val reduced_lines_set = items_inline.map(line => line.toSet & candidate_items.toSet)
    
    val reduced_lines_array = reduced_lines_set.map(line => line.toArray)
    
    val two_unique_tuple = reduced_lines_array.map(line =>line.combinations(2).toArray.map(line => (line(0), line(1))))
    
    val repeat_tuple = two_unique_tuple.map(line => line ++ line.map(pair => (pair._2, pair._1)))
    
    val flatten_pairs = repeat_tuple.flatMap(line => line)
    
    val pairs_reduce = flatten_pairs.map(line => (line, 1)).reduceByKey(_ + _).filter(pair => pair._2 >= 100)
    
    val candidate_pairs = pairs_reduce.map(line => line._1).collect().toSet
    
    val format_reduced_pairs = pairs_reduce.map(line => (line._1._1, (line._1._2, line._2)))
    
    val conf_pair = format_reduced_pairs.join(item_count).map(line => ((line._1, line._2._1._1), line._2._1._2.toFloat/line._2._2.toFloat))
    
    val sort_conf = conf_pair.sortBy(pairs => -pairs._2)
    
    val print_conf_pairs = sort_conf.take(5).map(pairs => pairs._1._1 + "=>" + pairs._1._2 + "  Conf: " + pairs._2.toString)
    
    print_conf_pairs.foreach(println)
    
    val del_repeat_pairs = two_unique_tuple.map(line => line.toSet & candidate_pairs).map(line => line.toArray)
    
    val merge = reduced_lines_array zip del_repeat_pairs
    
    val three_unique_tuple = merge.map(line => line._2.map(pair => line._1.map(item =>(pair, item)))).map(line => line.distinct)
    val flat_three_tuple = three_unique_tuple.flatMap(line => line).flatMap(line=>line)
    val three_tuple = flat_three_tuple.filter(line => (line._1._1!=line._2 && line._1._2!=line._2))
    
    //val three_unique_tuple = reduced_lines_array.map(line =>line.combinations(3).toArray.map(line => (line(0), line(1), line(2))))
    
    /*
    val repeat_three_tuple = three_unique_tuple.map(line => line.map(x => ((x._1, x._2), x._3)) ++ 
                                                                 line.map(x => ((x._1, x._3), x._2)) ++
                                                                 line.map(x => ((x._2, x._3), x._1)))
    */                                                             
    //val flat_three_tuple = repeat_three_tuple.flatMap(line => line)
    
    val three_tuple_reduce = three_tuple.map(line => (line, 1)).reduceByKey(_ + _).filter(pair => pair._2 >= 100)
    
    val format_three_tuple = three_tuple_reduce.map(line => (line._1._1, (line._1._2, line._2)))
    
    val conf_three_tuple = format_three_tuple.join(pairs_reduce).map(line => ((line._1, line._2._1._1), line._2._1._2.toFloat/line._2._2.toFloat))
    
    val sort_three_tuple_conf = conf_three_tuple.sortBy(line => -line._2)
    
    val print_three_tuple_conf = sort_three_tuple_conf.take(5).map(line => line._1._1 + "=>" + line._1._2 + "   conf:" + line._2.toString)
    
    print_three_tuple_conf.foreach(println)
    
  }
}
