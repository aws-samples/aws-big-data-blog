package com.amazonaws.proserv.ml

import com.typesafe.config.Config
import org.apache.spark.SparkContext
import spark.jobserver._
import org.apache.spark.mllib.recommendation.{ALS, Rating, MatrixFactorizationModel}

import scala.collection.mutable.ListBuffer


/**
  * Created by dgraeber on 8/4/2016.
  */
object LoadModelAndData extends SparkJob with NamedRddSupport{


  override def runJob(sc: SparkContext, jobConfig: Config): Any = {

    val movieLensHomeDir = jobConfig.getString("s3DataLoc")
    val modelHomeDir = jobConfig.getString("s3ModelLoc")

// Load the movies data (movies and movies with genre

    val movies = sc.textFile(movieLensHomeDir + "movies.dat").map { line =>
      val fields = line.split("::")
      // format: (movieId, movieName)
      (fields(0).toInt, fields(1))
    }//.collect.toMap

    val moviesWithGenres = sc.textFile(movieLensHomeDir + "movies.dat").map { line =>
      val fields = line.split("::")
      // format: (movieId, movieName, genre information)
      (fields(0).toInt, fields(2))
    }//.collect.toMap

    this.namedRdds.update("movies",movies)
    this.namedRdds.update("moviesWithGenres",moviesWithGenres)


/* Load the model from S3 */
    val model= MatrixFactorizationModel.load(sc,modelHomeDir)

    /* this persists to HDFS */
//    model.save(sc, "hdfs:///mnt/lib/spark-jobserver/modeldata/")


    /* This persists the RDD's of the model to context for fetching */
    //RDD[(Int,Array[Double])
    this.namedRdds.update("productFeatures",model.productFeatures)
    this.namedRdds.update("userFeatures",model.userFeatures)

    val rankInt= Array(model.rank )
    val rankRDD = sc.parallelize(rankInt)
    //RDD[(Int)
    this.namedRdds.update("rank",rankRDD)

  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid  //not really checking here...but probably should
}


object MoviesRecByGenre extends SparkJob with NamedRddSupport {
  override def runJob(sc: SparkContext, jobConfig: Config): Any = {

    val userId =jobConfig.getString("userId").toInt
    val genre = jobConfig.getString("genre")


    val moviesWithGenres = this.namedRdds.get[(Int, String)]("moviesWithGenres").get.collect.toMap
    val movies = this.namedRdds.get[(Int,Int)]("movies").get.collect.toMap
/* Use this if pulling model from HDFS */
    //val model = MatrixFactorizationModel.load(sc,"hdfs:///mnt/lib/spark-jobserver/modeldata/")


/* Use this if creating model from RDD's persisted in Context */
        val rank = this.namedRdds.get[(Int)]("rank").get.collect().toSeq
        val rankInt = rank.head
          val model = new MatrixFactorizationModel(rankInt,
            this.namedRdds.get[(Int,Array[Double])]("userFeatures").get,
            this.namedRdds.get[(Int,Array[Double])]("productFeatures").get
          )
    val comedyMovies = moviesWithGenres.filter(_._2.matches(".*"+genre+".*")).keys
    val candidates = sc.parallelize(comedyMovies.toSeq)

    val recommendations = model
      .predict(candidates.map((userId, _)))
      .collect()
      .sortBy(- _.rating)
      .take(5)

    //recommendations
    var recs = new ListBuffer[String]()
    var i = 1
    recommendations.foreach { r =>
      recs += "%2d".format(i) + ": " + movies(r.product)
      i += 1
    }
    recs.toList
  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid //not really checking here...but probably should
}

object MoviesRec extends SparkJob with NamedRddSupport {
  override def runJob(sc: SparkContext, jobConfig: Config): Any = {

    val userId =jobConfig.getString("userId").toInt

    val movies = this.namedRdds.get[(Int,Int)]("movies").get.collect.toMap

    /* Use this if pulling model from HDFS */
    //val model = MatrixFactorizationModel.load(sc,"hdfs:///mnt/lib/spark-jobserver/modeldata/")

    /* Use this if creating model from RDD's persisted in Context */
    val rank = this.namedRdds.get[(Int)]("rank").get.collect().toSeq
    val rankInt = rank.head
      val model = new MatrixFactorizationModel(rankInt,
        this.namedRdds.get[(Int,Array[Double])]("userFeatures").get,
        this.namedRdds.get[(Int,Array[Double])]("productFeatures").get
      )

    val candidates = sc.parallelize(movies.keys.toSeq)
    val recommendations = model
      .predict(candidates.map((userId, _)))
      .collect()
      .sortBy(- _.rating)
      .take(10)

    var recs = new ListBuffer[String]()
    var i = 1
    recommendations.foreach { r =>
      recs += "%2d".format(i) + ": " + movies(r.product)
      i += 1
    }

    recs.toList


  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid //not really checking here...but probably should
}

object TestParams extends SparkJob with NamedRddSupport {
  override def runJob(sc: SparkContext, jobConfig: Config): Any = {
    val movieLensHomeDir = jobConfig.getString("s3DataLoc")
    val modelHomeDir = jobConfig.getString("s3ModelLoc")

    val res = "Got "+movieLensHomeDir+" and "+modelHomeDir
    res
  }

  override def validate(sc: SparkContext, config: Config): SparkJobValidation = SparkJobValid //not really checking here...but probably should


}