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

package org.nlp4l.ltr.support.actors


import akka.actor.Actor
import akka.actor.Props
import javax.inject.Inject
import org.nlp4l.ltr.support.dao.LtrmodelDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.LtrannotationDAO
import org.nlp4l.ltr.support.dao.LtrqueryDAO
import org.nlp4l.ltr.support.dao.FeatureDAO
import java.util.Random
import akka.actor.ActorRef
import play.api.Logger
import org.nlp4l.ltr.support.models.Ltrconfig
import org.nlp4l.ltr.support.models.FeatureExtractDTOs
import org.nlp4l.ltr.support.models.FeatureExtractResults



class ProgressActor @Inject()(jdocFeatureDAO: DocFeatureDAO, 
                             featureDAO: FeatureDAO, 
                             ltrconfigDAO: LtrconfigDAO,
                             ltrmodelDAO: LtrmodelDAO,
                             ltrqueryDAO: LtrqueryDAO,
                             ltrannotationDAO: LtrannotationDAO) extends Actor {
  
  private val logger = Logger(this.getClass)
  
  override def receive: Receive = {
    case FeatureExtractStartMsg(dtos: FeatureExtractDTOs) => {
      context.actorOf(Props[FeatureActor]) ! FeatureExtractStartMsg(dtos)
    }
    case FeatureExtractSetProgressMsg(ltrid: Int, value: Int) => {
      logger.info("FeatureExtractSetProgressMsg received: " + ltrid + " [" + value + "]")
      // TODO DB store
      // DEMO
      FeatureProgressDB.set(ltrid, value)
    }
    case FeatureExtractGetProgressMsg(ltrid: Int) => {
      // TODO DB retrieve
      // DEMO
      val n = FeatureProgressDB.get(ltrid)
      logger.info("FeatureExtractGetProgressMsg received: " + ltrid + " [" + n + "]")
      sender ! n
    }
    case FeatureExtractSetResultMsg(ltrid: Int, result: FeatureExtractResults) => {
      logger.info("FeatureExtractSetProgressMsg received: " + ltrid + " [" + result + "]")
      // TODO DB store
    }
    case FeatureExtractClearResultMsg(ltrid: Int) => {
      // TODO DB retrieve
      // DEMO
      val n = FeatureProgressDB.set(ltrid, 0)
    }
  }
}

object ProgressActor {
  def props = Props[ProgressActor]
}

// Start a feature extraction
case class FeatureExtractStartMsg(dtos: FeatureExtractDTOs)
// Set a progress value of the feature extraction
case class FeatureExtractSetProgressMsg(ltrid: Int, value: Int)
// Get a progress value of the feature extraction
case class FeatureExtractGetProgressMsg(ltrid: Int)
// Set the result of the feature extraction
case class FeatureExtractSetResultMsg(ltrid: Int, result: FeatureExtractResults)
// Clear a result of the feature extraction
case class FeatureExtractClearResultMsg(ltrid: Int)


// DEMO
object FeatureProgressDB {
  private var p: Map[Int, Int] = Map()
  def set(ltrid: Int, n: Int): Unit = {
    p = p + (ltrid -> n)
  }
  def get(ltrid: Int): Int = {
    p.getOrElse(ltrid, 0)
  }
}


