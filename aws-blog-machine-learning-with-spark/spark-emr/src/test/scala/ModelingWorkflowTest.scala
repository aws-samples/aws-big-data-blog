class ModelingWorkflowTest extends org.scalatest.FunSuite {

  test("worklfow") {
    val subject = ModelingWorkflow.runWorkflow("src/test/resources", "/tmp/")
    assert(subject.numClasses.equals(2))
    assert(subject.weights.size.equals(692))
  }


}
