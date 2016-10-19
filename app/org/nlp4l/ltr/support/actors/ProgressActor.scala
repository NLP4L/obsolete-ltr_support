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
import org.nlp4l.ltr.support.dao.LtrfeatureDAO
import java.util.Random
import akka.actor.ActorRef
import play.api.Logger
import org.nlp4l.ltr.support.models.Ltrconfig
import org.nlp4l.ltr.support.models.FeatureExtractDTOs
import org.nlp4l.ltr.support.models.FeatureExtractResults
import scala.concurrent.Await
import org.nlp4l.ltr.support.models.FeatureProgress
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import scala.util.Failure
import scala.util.Success
import org.nlp4l.ltr.support.models.DocFeature
import org.nlp4l.ltr.support.dao.FeatureProgressDAO
import org.nlp4l.ltr.support.dao.LtrfeatureDAO
import org.nlp4l.ltr.support.models.Ltrfeature



class ProgressActor @Inject()(docFeatureDAO: DocFeatureDAO, 
                             ltrfeatureDAO: LtrfeatureDAO, 
                             ltrconfigDAO: LtrconfigDAO,
                             ltrmodelDAO: LtrmodelDAO,
                             ltrqueryDAO: LtrqueryDAO,
                             ltrannotationDAO: LtrannotationDAO,
                             featureProgressDAO: FeatureProgressDAO) extends Actor {
  
  private val logger = Logger(this.getClass)
  
  override def receive: Receive = {
    case FeatureExtractStartMsg(dtos: FeatureExtractDTOs) => {
      logger.info("FeatureExtractStartMsg received: " + dtos.ltrid)
      logger.debug("FeatureExtractStartMsg received: " + dtos)
      context.actorOf(Props[FeatureActor]) ! FeatureExtractStartMsg(dtos)
    }
    case FeatureExtractSetProgressMsg(ltrid: Int, value: Int, message: String) => {
      logger.debug("FeatureExtractSetProgressMsg received: " + ltrid + " [" + value + "]")
      
      // FeatureProgressDB.set(ltrid, value) // for DEBUG
      val delf = featureProgressDAO.deleteByLtrid(ltrid)
      Await.result(delf, scala.concurrent.duration.Duration.Inf)
      val insf = featureProgressDAO.insert(FeatureProgress(None, ltrid, value, message))
      Await.result(insf, scala.concurrent.duration.Duration.Inf)
    }
    case FeatureExtractGetProgressMsg(ltrid: Int) => {
      // val n = FeatureProgressDB.get(ltrid) // for DEBUG
      val f = featureProgressDAO.getByLtrid(ltrid)
      Await.ready(f, scala.concurrent.duration.Duration.Inf)
      f.value.get match {
        case Success(featureProgress) => {
          val n = featureProgress.progress
          logger.debug("FeatureExtractGetProgressMsg received: " + ltrid + " [" + n + "]")
          sender ! n
        }
        case Failure(ex) => {
          val n = 0
          logger.debug("FeatureExtractGetProgressMsg received: " + ltrid + " [" + n + "]")
          sender ! n
        }
      }      
    }
    case FeatureExtractGetProgressMessageMsg(ltrid: Int) => {
      // val n = FeatureProgressDB.getMessage(ltrid) // for DEBUG
      val f = featureProgressDAO.getByLtrid(ltrid)
      Await.ready(f, scala.concurrent.duration.Duration.Inf)
      f.value.get match {
        case Success(featureProgress) => {
          val msg = featureProgress.message
          logger.debug("FeatureExtractGetProgressMessageMsg received: " + ltrid + " [" + msg + "]")
          sender ! msg
        }
        case Failure(ex) => {
          val n = 0
          logger.debug("FeatureExtractGetProgressMsg received: " + ltrid + " [" + n + "]")
          sender ! n
        }
      }      
    }
    case FeatureExtractSetResultMsg(ltrid: Int, result: FeatureExtractResults) => {
      logger.info("FeatureExtractSetProgressMsg received: " + ltrid)
      logger.debug("FeatureExtractSetProgressMsg received: "+ result)
      
      val fdelf = ltrfeatureDAO.deleteByLtrid(ltrid)
      Await.result(fdelf, scala.concurrent.duration.Duration.Inf)
      var fidmap: Map[Int, Int] = Map()
      result.feature.zipWithIndex.foreach { case (fname,n) =>
        val finsf = ltrfeatureDAO.insert(Ltrfeature(None, ltrid, fname))
        val fins = Await.result(finsf, scala.concurrent.duration.Duration.Inf)
        fins.fid map {fid =>
          fidmap = fidmap + (n -> fid)
        }
      }
      result.results map { docf =>
        fidmap.get(docf.fid) map {fid =>
          val f = docFeatureDAO.insert(DocFeature(None, fid, docf.qid, docf.docid, docf.value))
          Await.result(f, scala.concurrent.duration.Duration.Inf)
        }
      }
      
    }
    case FeatureExtractClearResultMsg(ltrid: Int) => {
      // val n = FeatureProgressDB.set(ltrid, 0) // for DEBUG
      val qn = ltrqueryDAO.totalCountByLtrid(ltrid)
      val qlist = ltrqueryDAO.fetchByLtrid(ltrid, "qid", "asc", 0, qn)
      qlist map { q =>
        q.qid map {qid =>
          val delf = docFeatureDAO.deleteByQuid(qid)
          Await.result(delf, scala.concurrent.duration.Duration.Inf)
        }
      }
      val delf = featureProgressDAO.deleteByLtrid(ltrid)
      Await.result(delf, scala.concurrent.duration.Duration.Inf)
    }
  }
}

object ProgressActor {
  def props = Props[ProgressActor]
}

// Start a feature extraction
case class FeatureExtractStartMsg(dtos: FeatureExtractDTOs)
// Set a progress value of the feature extraction
case class FeatureExtractSetProgressMsg(ltrid: Int, value: Int, message: String)
// Get a progress value of the feature extraction
case class FeatureExtractGetProgressMsg(ltrid: Int)
// Get a progress message of the feature extraction
case class FeatureExtractGetProgressMessageMsg(ltrid: Int)
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
  def getMessage(ltrid: Int): String = {
    "Error Error Error"
  }
}


