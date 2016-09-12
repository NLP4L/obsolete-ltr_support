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

package org.nlp4l.ltr.support.controllers

import java.util.UUID
import java.nio.file.Files
import java.nio.file.Paths
import java.io.File
import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.convert.WrapAsScala._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.FeatureDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.LtrmodelDAO
import org.nlp4l.ltr.support.dao.LtrqueryDAO
import org.nlp4l.ltr.support.dao.LtrsuperviseDAO
import org.nlp4l.ltr.support.models.ActionResult
import org.nlp4l.ltr.support.models.DbModels._
import org.nlp4l.ltr.support.models.Ltrconfig
import org.nlp4l.ltr.support.models.Ltrquery
import org.nlp4l.ltr.support.models.ViewModels._
import com.google.inject.name.Named
import akka.actor.ActorRef
import akka.actor.ActorSystem
import javax.inject.Inject
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import org.nlp4l.ltr.support.actors.ProgressGetMsg_Feature
import org.nlp4l.ltr.support.actors.StartMsg_Feature
import akka.actor.Props
import org.nlp4l.ltr.support.actors.ProgressActor
import akka.pattern.AskableActorRef
import akka.util.Timeout
import java.util.concurrent.TimeUnit
import org.nlp4l.ltr.support.actors.ClearMsg_Feature

class LtrController @Inject()(docFeatureDAO: DocFeatureDAO, 
                             featureDAO: FeatureDAO, 
                             ltrconfigDAO: LtrconfigDAO,
                             ltrmodelDAO: LtrmodelDAO,
                             ltrqueryDAO: LtrqueryDAO,
                             ltrsuperviseDAO: LtrsuperviseDAO ,
                             @Named("progress-actor") progressActor: ActorRef ) extends Controller {


  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  val pa = new AskableActorRef(progressActor)
  
  def saveLtrConfig(ltrid: Int) = Action.async(parse.json) { request =>
    val data = request.body
    val name = (data \ "name").as[String]
    val superviseType = (data \ "superviseType").as[String]
    val modelFactryClassName = (data \ "modelFactryClassName").as[String]
    val modelFactoryClassSettings = (data \ "modelFactoryClassSettings").as[String]
    val searchUrl = (data \ "searchUrl").as[String]
    val featureUrl = (data \ "featureUrl").as[String]
    val docUniqField = (data \ "docUniqField").as[String]
    val labelMax = (data \ "labelMax").as[String]

    if (name.isEmpty) {
      Future.successful(BadRequest("Name cannot be empty."))
    } else {
      val newLtr: Ltrconfig = Ltrconfig(Some(ltrid), name, superviseType, modelFactryClassName, Some(modelFactoryClassSettings), searchUrl, featureUrl, docUniqField, labelMax.toInt)
      val f: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
      Await.ready(f, scala.concurrent.duration.Duration.Inf)
      f.value.get match {
        case Success(ltr) => {
          ltrconfigDAO.update( newLtr ) map {
            res => {
              val jsonResponse = Json.toJson(newLtr)
              Ok(jsonResponse)
            }
          } recover {
            case e => InternalServerError("Add failed. " + e.getMessage)
          }
        }
        case Failure(ex) => {
          ltrconfigDAO.insert( newLtr ) map {
            res => {
              val jsonResponse = Json.toJson(res)
              Ok(jsonResponse)
            }
          } recover {
            case e => InternalServerError("Add failed. " + e.getMessage)
          }
        }
      }
      
    }
  }

  def deleteLtrConfig(ltrid: Int) = Action.async {
    val f: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
    val ltr = Await.result(f, scala.concurrent.duration.Duration.Inf)
    ltrconfigDAO.delete(ltrid) map {
      case (a) => {
        Ok(Json.toJson(ActionResult(true, Seq("success"))))
      }
    } recover {
      case e => InternalServerError("Delete failed. " + e.getMessage)
    }
  }


  def saveQuery(ltrid: Int) = Action(parse.multipartFormData) { request =>
    request.body.file("query") map { file =>
      val uuid = UUID.randomUUID().toString
      val temp = new File(s"/tmp/$uuid")
      file.ref.moveTo(temp, replace = true)
      val tempPath = Paths.get(temp.getAbsolutePath)
      val lines = Files.readAllLines(tempPath).toList
      lines.foreach( l => {
        val ltrquery = Ltrquery(None, l, ltrid, false)
        ltrqueryDAO.insert(ltrquery)
      })
    }
    Redirect("/ltrdashboard/" + ltrid + "/query")
  }

  def listQuery(ltrid: Int) = Action { request =>
    val offset = request.getQueryString("offset") match {
      case Some(x) if x != "" => x.toInt
      case _ => 0
    }
    val size = request.getQueryString("limit") match {
      case Some(x) => x.toInt
      case _ => 10
    }
    val sort = request.getQueryString("sort") match {
      case Some(c) => c
    }
    val order = request.getQueryString("order") match {
      case Some(c) => c
      case _ => "asc"
    }
    val total = ltrqueryDAO.totalCountByLtrid(ltrid)
    val res = ltrqueryDAO.fetchByLtrid(ltrid, sort, order, offset, size)
    val jsonResponse = Json.obj(
      "total" -> total,
      "rows" -> Json.toJson(res)
    )
    Ok(jsonResponse)
  }

  def startFeatureEtraction(ltrid: Int) = Action {
    progressActor ! StartMsg_Feature(ltrid)
    Ok(Json.toJson(ActionResult(true, Seq("started"))))
  }
  
  def getFeatureProgress(ltrid: Int) = Action.async {
    val f = pa ? ProgressGetMsg_Feature(ltrid)
    f.map(result => Ok(result.toString()))
  }

  def clearFeatureProgress(ltrid: Int) = Action {
    progressActor ! ClearMsg_Feature(ltrid)
    Ok(Json.toJson(ActionResult(true, Seq("cleared"))))
  }
  
}



