/*
 * Copyright 2015 org.NLP4L
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nlp4l.ltr.support.procs

import akka.actor.{ActorRef, actorRef2Scala}
import com.typesafe.config.{Config, ConfigFactory}
import org.nlp4l.ltr.support.actors._
import play.api.Logger

class TrainingExecutor(sender: ActorRef) extends TrainingProgressReport {
  
  private val logger = Logger(this.getClass)
  
  def execute(trainingRequest: TrainingRequest) = {
    val anMap = trainingRequest.ltrannotations.map(a => (a.qid, a.docid) -> a.label).toMap
    val dfMap = trainingRequest.docFeatures.map(a => (a.qid, a.docid, a.fid) -> a.value).toMap
    val fkeys = trainingRequest.docFeatures.map(a => (a.qid, a.docid)).distinct.sorted

    val features: Array[Vector[Float]] = fkeys.map(fkey => {
      trainingRequest.selectedFeatures.map(f => {
        dfMap.getOrElse((fkey._1, fkey._2, f.fid.get), 0.0F)
      }).toVector
    }).toArray

    val labels: Array[Int] = fkeys.map(fkey => {
      anMap.getOrElse((fkey._1, fkey._2),0)
    }).toArray

    val featureNames = trainingRequest.selectedFeatures.map(_.name).toArray

    try {
      val settings = trainingRequest.ltrconfig.trainerFactoryClassSettings
      val config = if (settings.isDefined) ConfigFactory.parseString(settings.get) else ConfigFactory.empty()

      val constructor = Class.forName(trainingRequest.ltrconfig.trainerFactryClassName).getConstructor(classOf[Config])
      val factory = constructor.newInstance(config).asInstanceOf[TrainerFactory]
      val trainer = factory.getInstance()
      val progressSender = new TrainingProgressSender(sender, trainingRequest.ltrid, trainingRequest.runid)

      val result = trainer.train(featureNames, features, labels, trainingRequest.ltrconfig.labelMax + 1, progressSender)

      val trainingResult = TrainingResult(trainingRequest.ltrid, trainingRequest.runid, 1, "", result)
      sender ! TrainingSetResultMsg(trainingResult)

    } catch {
      case e: Exception => {
        logger.error(e.getMessage, e)
        val trainingResult = TrainingResult(trainingRequest.ltrid, trainingRequest.runid, -1, e.getMessage, "")
        sender ! TrainingSetResultMsg(trainingResult)
      }
    }
  }
}

class TrainingProgressSender(sender: ActorRef, ltrid: Int, runid: Int) extends TrainingProgress {
  def report(progress: Int): Unit = {
    sender ! TrainingSetProgressMsg(ltrid, runid, progress)
  }
}