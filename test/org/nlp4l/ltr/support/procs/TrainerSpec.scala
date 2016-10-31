package org.nlp4l.ltr.support.procs

import akka.actor.ActorRef
import com.typesafe.config.ConfigFactory
import org.junit.runner.RunWith
import org.nlp4l.ltr.support.actors.TrainingSetProgressMsg
import org.nlp4l.ltr.support.models.LtrModels._
import org.nlp4l.ltr.support.procs.PRankTrainerFactory
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TrainerSpec extends Specification {

  "Trainer" should {

    val settings = "{}"

    "execute training" in {
      val featureNames = Array("f1","f2")
      val features: Array[Vector[Float]] = Array(
        Vector(1, 7), Vector(2, 7), Vector(3, 7), Vector(2, 8),
        Vector(3, 4), Vector(5, 4), Vector(3, 5), Vector(4, 5), Vector(5, 5),
        Vector(7, 2), Vector(7, 3), Vector(8, 3), Vector(7, 4), Vector(8, 4), Vector(8, 5),
        Vector(10, 1), Vector(10, 2), Vector(11, 2), Vector(12, 2), Vector(11, 3), Vector(12, 3)
      )
      val labels = Array(
        1, 1, 1, 1,
        2, 2, 2, 2, 2,
        3, 3, 3, 3, 3, 3,
        4, 4, 4, 4, 4, 4
      )
      val maxLabel = 4
      val prank = new PRank(features, labels, featureNames.length, maxLabel, 10005, new TestTrainingProgressSender())
      val wb = prank.train()

      prank.dumpResult()

      // test
      prank.test(Vector(7.5F, 3.5F))
      prank.test(Vector(7.5F, 8F))
      prank.test(Vector(3F, 3F))
      prank.test(Vector(0.5F, 5F))
      prank.test(Vector(12F, 4F))

      val factory = new PRankTrainerFactory(ConfigFactory.empty())
      val trainer = factory.getInstance()
      val result = trainer.train(featureNames, features, labels, maxLabel, new TestTrainingProgressSender())
      result != ""
    }

  }
}

class TestTrainingProgressSender() extends TrainingProgress {
  def report(progress: Int): Unit = {
    println("progress: " + progress)
  }
}