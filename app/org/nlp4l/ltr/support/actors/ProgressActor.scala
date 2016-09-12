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



class ProgressActor @Inject()(jdocFeatureDAO: DocFeatureDAO, 
                             featureDAO: FeatureDAO, 
                             ltrconfigDAO: LtrconfigDAO,
                             ltrmodelDAO: LtrmodelDAO,
                             ltrqueryDAO: LtrqueryDAO,
                             ltrannotationDAO: LtrannotationDAO) extends Actor {
  
  private val logger = Logger(this.getClass)
  
  override def receive: Receive = {
    case StartMsg_Feature(ltrid: Int) => {
      context.actorOf(Props[FeatureActor]) ! StartMsg_Feature(ltrid)
    }
    case ProgressSetMsg_Feature(ltrid: Int, value: Int) => {
      logger.info("ProgressSetMsg_Feature received: " + ltrid + " [" + value + "]")
      // TODO DB store
      // DEMO
      FeatureProgressDB.set(ltrid, value)
    }
    case ProgressGetMsg_Feature(ltrid: Int) => {
      // TODO DB retrieve
      // DEMO
      val n = FeatureProgressDB.get(ltrid)
      logger.info("ProgressGetMsg_Feature received: " + ltrid + " [" + n + "]")
      sender ! n
    }
    case ClearMsg_Feature(ltrid: Int) => {
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
case class StartMsg_Feature(ltrid: Int)
// Set a progress value of the feature extraction
case class ProgressSetMsg_Feature(ltrid: Int, value: Int)
// Get a progress value of the feature extraction
case class ProgressGetMsg_Feature(ltrid: Int)
// Clear a result of the feature extraction
case class ClearMsg_Feature(ltrid: Int)


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


