import com.intentmedia.mario.Pipeline._
import org.apache.spark.mllib.classification.{LogisticRegressionModel, LogisticRegressionWithLBFGS}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD
import org.apache.spark.{SparkConf, SparkContext}

object ModelingWorkflow {

  val conf = new SparkConf()
    .setAppName("modelingJob")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  val sc = new SparkContext(conf)


  def runWorkflow(inputPath: String, outputPath: String): LogisticRegressionModel = {

    val trainingDataPath = inputPath + "/training/sample_binary_classification_training_data.txt"
    val testingDataPath = inputPath + "/testing/sample_binary_classification_testing_data.txt"
    val currentOutputPath = outputPath + System.currentTimeMillis()

    for {
      trainingDataStep <- pipe(loadFeatures(trainingDataPath))
      testingDataStep <- pipe(loadFeatures(testingDataPath))

      modelStep <- pipe(learnModel, trainingDataStep)

      predictionsAndLabelsStep <- pipe(predictAndLabel, testingDataStep, modelStep)

      metricsStep <- pipe(metrics, predictionsAndLabelsStep)
      saveMetricsStep <- pipe(saveMetrics(currentOutputPath), metricsStep)

      saveModelStep <- pipe(saveModel(currentOutputPath), modelStep)

    } yield modelStep.runWith(saveMetricsStep, saveModelStep)
  }

  def loadFeatures(inputPath: String) = MLUtils.loadLibSVMFile(sc, inputPath)

  def learnModel(trainingData: RDD[LabeledPoint]) = new LogisticRegressionWithLBFGS()
    .setNumClasses(2)
    .run(trainingData)

  def predictAndLabel(testingData: RDD[LabeledPoint], model: LogisticRegressionModel) =
    testingData.map { case LabeledPoint(label, features) =>
      val prediction = model.predict(features)
      (prediction, label)
    }

  def metrics(predictionsAndLabels: RDD[(Double, Double)]) = new MulticlassMetrics(predictionsAndLabels)

  def saveModel(outputPath: String)(model: LogisticRegressionModel) = {
    val modelPath = outputPath + "/model"
    model.save(sc, modelPath)
  }

  def saveMetrics(outputPath: String)(metrics: MulticlassMetrics) = {
    val precision = metrics.precision
    val recall = metrics.recall
    val metricsPath = outputPath + "/metrics"
    val metricsStringRDD = sc.parallelize(List(precision, recall))
    metricsStringRDD.saveAsTextFile(metricsPath)
  }

  def main(args: Array[String]): Unit = {
    val inputPath = args(0)
    val outputPath = args(1)
    val model = runWorkflow(inputPath, outputPath)
    println(model.weights)
  }

}


