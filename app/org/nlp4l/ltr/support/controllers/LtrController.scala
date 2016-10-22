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

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import java.util.concurrent.TimeUnit

import scala.collection.JavaConversions.asScalaBuffer
import scala.collection.convert.WrapAsScala._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success
import org.nlp4l.ltr.support.actors.FeatureExtractClearResultMsg
import org.nlp4l.ltr.support.actors.FeatureExtractGetProgressMsg
import org.nlp4l.ltr.support.actors.FeatureExtractStartMsg
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.LtrfeatureDAO
import org.nlp4l.ltr.support.dao.LtrannotationDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.LtrmodelDAO
import org.nlp4l.ltr.support.dao.LtrqueryDAO
import org.nlp4l.ltr.support.models._
import org.nlp4l.ltr.support.models.ViewModels._
import org.nlp4l.ltr.support.models.DbModels._
import org.nlp4l.ltr.support.models.LtrModels._
import com.google.inject.name.Named
import akka.actor.ActorRef
import akka.pattern.AskableActorRef
import akka.util.Timeout
import javax.inject.Inject

import org.joda.time.DateTime
import play.api.libs.json.JsObject
import play.api.libs.json.JsValue.jsValueToJsLookup
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.mvc.Action
import play.api.mvc.Controller
import org.nlp4l.ltr.support.dao.FeatureProgressDAO
import org.nlp4l.ltr.support.actors.FeatureExtractGetProgressMessageMsg
import org.nlp4l.ltr.support.procs.PRankTrainerFactory

class LtrController @Inject()(docFeatureDAO: DocFeatureDAO, 
                             ltrfeatureDAO: LtrfeatureDAO, 
                             ltrconfigDAO: LtrconfigDAO,
                             ltrmodelDAO: LtrmodelDAO,
                             ltrqueryDAO: LtrqueryDAO,
                             ltrannotationDAO: LtrannotationDAO,
                             featureProgressDAO: FeatureProgressDAO,
                             @Named("progress-actor") progressActor: ActorRef ) extends Controller {


  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)
  val pa = new AskableActorRef(progressActor)

  def saveLtrConfig(ltrid: Int) = Action.async(parse.json) { request =>
    val data = request.body
    val name = (data \ "name").as[String]
    val annotationType = (data \ "annotationType").as[String]
    val modelFactryClassName = (data \ "modelFactryClassName").as[String]
    val modelFactoryClassSettings = (data \ "modelFactoryClassSettings").as[String]
    val searchUrl = (data \ "searchUrl").as[String]
    val featureExtractUrl = (data \ "featureExtractUrl").as[String]
    val featureProgressUrl = (data \ "featureProgressUrl").as[String]
    val featureRetrieveUrl = (data \ "featureRetrieveUrl").as[String]
    val docUniqField = (data \ "docUniqField").as[String]
    val docTitleField = (data \ "docTitleField").as[String]
    val docBodyField = (data \ "docBodyField").as[String]
    val labelMax = (data \ "labelMax").as[String]

    if (name.isEmpty) {
      Future.successful(BadRequest("Name cannot be empty."))
    } else {
      val newLtr: Ltrconfig = Ltrconfig(Some(ltrid), name, annotationType, modelFactryClassName, Some(modelFactoryClassSettings), searchUrl, featureExtractUrl, featureProgressUrl, featureRetrieveUrl, docUniqField, docTitleField, docBodyField, labelMax.toInt)
      val f: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
      Await.ready(f, scala.concurrent.duration.Duration.Inf)
      f.value.get match {
        case Success(ltr) => {
          ltrconfigDAO.update(newLtr) map {
            res => {
              val jsonResponse = Json.toJson(newLtr)
              Ok(jsonResponse)
            }
          } recover {
            case e => InternalServerError("Add failed. " + e.getMessage)
          }
        }
        case Failure(ex) => {
          ltrconfigDAO.insert(newLtr) map {
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
    ltrqueryDAO.clearCheckedFlg(ltrid)
    val queryList = ltrqueryDAO.fetchByLtrid(ltrid, "qid", "asc", 0, Integer.MAX_VALUE)
    queryList.foreach(q => {
      ltrannotationDAO.deleteByQid(q.qid.get)
      val f: Future[Int] = ltrqueryDAO.delete(q.qid.get)
      Await.ready(f, scala.concurrent.duration.Duration.Inf)
    })
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
      lines.foreach(l => {
        if (!l.trim().isEmpty) {
          val ltrquery = Ltrquery(None, l, ltrid, false)
          val f = ltrqueryDAO.insert(ltrquery)
          Await.ready(f, scala.concurrent.duration.Duration.Inf)
        }
      })
    }
    Redirect("/ltrdashboard/" + ltrid + "/query")
  }

  def clearAllAnnotation(ltrid: Int) = Action { request =>
    ltrqueryDAO.clearCheckedFlg(ltrid)
    val queryList = ltrqueryDAO.fetchByLtrid(ltrid, "qid", "asc", 0, Integer.MAX_VALUE)
    queryList.foreach(q => {
      ltrannotationDAO.deleteByQid(q.qid.get)
    })
    Redirect("/ltrdashboard/" + ltrid + "/query")
  }

  def clearAnnotation(ltrid: Int, qid : Int) = Action { request =>
    ltrannotationDAO.deleteByQid(qid)
    val fq: Future[Ltrquery] = ltrqueryDAO.get(qid)
    val ltrquery = Await.result(fq, scala.concurrent.duration.Duration.Inf)
    val fu: Future[Int] = ltrqueryDAO.update(ltrquery.copy(checked_flg = false))
    Await.ready(fu, scala.concurrent.duration.Duration.Inf)
    Ok(Json.toJson(ActionResult(true, Seq("cleared"))))
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
      case _ => "qid"
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

  def nextQuery(ltrid: Int, qid : Int) = Action {
    val ltrquery =ltrqueryDAO.fetchNext(ltrid, qid) match {
      case Some(x) => x
      case _ => Ltrquery(Some(0), "", ltrid, false)
    }
    Redirect("/ltrdashboard/" + ltrid + "/annotation/" + ltrquery.qid.get)
  }

  def deleteQuery(ltrid: Int, qid : Int) = Action.async {
    ltrannotationDAO.deleteByQid(qid)
    val f: Future[Int] = ltrqueryDAO.delete(qid)
    Await.ready(f, scala.concurrent.duration.Duration.Inf)
    Future.successful(Ok(Json.toJson(ActionResult(true, Seq("success")))))
  }

  def search(ltrid: Int, qid: Int) = Action { request =>
    val fc: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
    val ltrconfig = Await.result(fc, scala.concurrent.duration.Duration.Inf)

    val solrSearch = new SolrSearch(ltrconfig.searchUrl)
    val queryStr = request.getQueryString("q") match {
      case Some(q) => q
      case _ => {
        val fq: Future[Ltrquery] = ltrqueryDAO.get(qid)
        val ltrquery = Await.result(fq, scala.concurrent.duration.Duration.Inf)
        ltrquery.query
      }
    }
    val solrRes = solrSearch.search(queryStr)
    if (solrRes.statusSuccess) {
      val fl = ltrannotationDAO.getByQid(qid)
      val currentLabels = Await.result(fl, scala.concurrent.duration.Duration.Inf)
      val labelMap: Map[String, Int] = currentLabels.map(l => l.docid -> l.label).toMap
      val idField = ltrconfig.docUniqField
      val titleField = ltrconfig.docTitleField
      val bodyField = ltrconfig.docBodyField
      val docList = solrRes.docs.map(doc => {
        val idText = solrRes.getFirstValueAsString(doc, idField)
        val titleTextHL = solrRes.getHighlighting(idText,titleField)
        val titleText =
          if (titleTextHL != null) titleTextHL
          else {
            val titleText = solrRes.getValueAsString(doc, titleField, "...")
            if (titleText == null) ""
            else titleText
          }
        val bodyTextHL = solrRes.getHighlighting(idText,bodyField)
        val bodyText =
          if (bodyTextHL != null) bodyTextHL
          else {
            val bodyText = solrRes.getValueAsString(doc, bodyField, "...")
            if (bodyText == null) ""
            else if (bodyText.length > 300) bodyText.take(300) + "..."
            else bodyText
          }
        Json.obj(
          "id" -> idText,
          "label" -> Json.toJson(labelMap.getOrElse(idText, 0)),
          "title" -> titleText,
          "body" -> bodyText
        )
      })
      val jsonResponse = Json.obj(
        "status" -> 0,
        "rows" -> docList
      )
      Ok(jsonResponse)
    } else {
      val jsonResponse = Json.obj(
        "status" -> solrRes.status,
        "msg" -> solrRes.errorMsg
      )
      Ok(jsonResponse)
    }
  }

  def searchById(ltrid: Int) = Action { request =>
    val fc: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
    val ltrconfig = Await.result(fc, scala.concurrent.duration.Duration.Inf)

    val solrSearch = new SolrSearch(ltrconfig.searchUrl)
    val queryStr = ltrconfig.docUniqField + ":\"" + request.getQueryString("id").get + "\""
    val solrRes = solrSearch.search(queryStr)
    val idField = ltrconfig.docUniqField
    val titleField = ltrconfig.docTitleField
    val bodyField = ltrconfig.docBodyField
    val doc = solrRes.docs.head
    val jsonResponse = doc
    Ok(jsonResponse)
  }

  def saveLabels(ltrid: Int, qid: Int) = Action(parse.json) { request =>
    val data = request.body
    val query = (data \ "query").as[String]
    val labels = (data \ "labels").as[Seq[JsObject]].map( obj =>
      (obj.value.get("docId").get.as[String], obj.value.get("label").get.as[Int])
    )
    val saveQid = if (qid == 0) {
      val ltrquery = Ltrquery(None, query, ltrid, false)
      val f = ltrqueryDAO.insert(ltrquery)
      val newQuery = Await.result(f, scala.concurrent.duration.Duration.Inf)
      newQuery.qid.get
    } else {
      qid
    }
    ltrannotationDAO.deleteByQid(saveQid)
    val list = labels.map(label => {
      Ltrannotation(saveQid, label._1, label._2)
    })
    ltrannotationDAO.insertList(list)
    val fq: Future[Ltrquery] = ltrqueryDAO.get(saveQid)
    val ltrquery = Await.result(fq, scala.concurrent.duration.Duration.Inf)
    val fu: Future[Int] = ltrqueryDAO.update(ltrquery.copy(checked_flg = true))
    Await.ready(fu, scala.concurrent.duration.Duration.Inf)
    val jsonResponse = Json.obj(
      "qid" -> saveQid
    )
    Ok(jsonResponse)
  }

  def startFeatureEtraction(ltrid: Int) = Action {
    val f: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
    val ltr = Await.result(f, scala.concurrent.duration.Duration.Inf)
    
    val qCount = ltrqueryDAO.totalCountByLtrid(ltrid)
    val qlistf = ltrqueryDAO.fetch("qid", "asc", 0, qCount)
    val qlist = Await.result(qlistf, scala.concurrent.duration.Duration.Inf)
    
    var dtos: List[FeatureExtractDTO] = List()
    qlist.map { q =>
      val docsf = ltrannotationDAO.getByQid(q.qid.getOrElse(0))
      val docs = Await.result(docsf, scala.concurrent.duration.Duration.Inf)
      val dto: FeatureExtractDTO = FeatureExtractDTO(q.qid.getOrElse(0), q.query, docs.map(_.docid).toList)
      dtos = dtos :+ dto
    }
    val fedtos = FeatureExtractDTOs(ltr.ltrid.getOrElse(0), ltr.featureExtractUrl, ltr.featureProgressUrl, ltr.featureRetrieveUrl, ltr.docUniqField, dtos)
    
    progressActor ! FeatureExtractStartMsg(fedtos)
    Ok(Json.toJson(ActionResult(true, Seq("started"))))
  }
  
  def getFeatureProgress(ltrid: Int) = Action.async {
    val f = pa ? FeatureExtractGetProgressMsg(ltrid)
    f.map(result => Ok(result.toString()))
  }

  def getFeatureProgressMessage(ltrid: Int) = Action.async {
    val f = pa ? FeatureExtractGetProgressMessageMsg(ltrid)
    f.map(result => Ok(result.toString()))
  }
  
  def clearFeatureProgress(ltrid: Int) = Action {
    progressActor ! FeatureExtractClearResultMsg(ltrid)
    Ok(Json.toJson(ActionResult(true, Seq("cleared"))))
  }

  def startModelCreation(ltrid: Int) = Action { request =>
    val f: Future[Ltrconfig] = ltrconfigDAO.get(ltrid)
    val ltr = Await.result(f, scala.concurrent.duration.Duration.Inf)
    val fr: Future[Int] = ltrmodelDAO.nextRunId(ltrid)
    val runid = Await.result(fr, scala.concurrent.duration.Duration.Inf)
    val feature_list = request.getQueryString("features").get
    val ltrmodel: Ltrmodel = Ltrmodel(None,ltrid,runid,feature_list,None,0,0,Some(new DateTime()),None)
    val fm: Future[Ltrmodel] = ltrmodelDAO.insert(ltrmodel)
    val newModel = Await.result(fm, scala.concurrent.duration.Duration.Inf)

    val ff = ltrfeatureDAO.fetchByLtrid(ltrid)
    val featureList: Seq[Ltrfeature] = Await.result(ff, scala.concurrent.duration.Duration.Inf).sortBy(_.fid)
    val selected: Array[Int] = feature_list.split(",").map(f => f.toInt)
    val selectedFeatureList = featureList.filter(f => selected.contains(f.fid.get))

    val xfeatures: Array[Vector[Float]] = Array(
      Vector(1, 7), Vector(2, 7), Vector(3, 7), Vector(2, 8),
      Vector(3, 4), Vector(5, 4), Vector(3, 5), Vector(4, 5), Vector(5, 5),
      Vector(7, 2), Vector(7, 3), Vector(8, 3), Vector(7, 4), Vector(8, 4), Vector(8, 5),
      Vector(10, 1), Vector(10, 2), Vector(11, 2), Vector(12, 2), Vector(11, 3), Vector(12, 3)
    )
    val ylabels = Array(
      1, 1, 1, 1,
      2, 2, 2, 2, 2,
      3, 3, 3, 3, 3, 3,
      4, 4, 4, 4, 4, 4
    )
    val fd = docFeatureDAO.fetchByFids(selected)
    val docFeatures: Seq[DocFeature] = Await.result(fd, scala.concurrent.duration.Duration.Inf)

    val fl = ltrannotationDAO.fetchAll() // TODO: filter
    val ltrannotations = Await.result(fl, scala.concurrent.duration.Duration.Inf)

    val anMap = ltrannotations.map(a => (a.qid, a.docid) -> a.label).toMap
    val dfMap = docFeatures.map(a => (a.qid, a.docid, a.fid) -> a.value).toMap
    val fkeys = docFeatures.map(a => (a.qid, a.docid)).distinct.sorted

    val features: Array[Vector[Float]] = fkeys.map(fkey => {
      selectedFeatureList.map(f => {
        dfMap.getOrElse((fkey._1, fkey._2, f.fid.get), 0.0F)
      }).toVector
    }).toArray

    val labels: Array[Int] = fkeys.map(fkey => {
        anMap.getOrElse(((fkey._1, fkey._2)),0)
    }).toArray

    val featureNames = selectedFeatureList.map(_.name).toArray

    // for now, we call it directly.
    val factory = new PRankTrainerFactory(null)
    val trainer = factory.getInstance()
    val result = trainer.train(featureNames, features, labels, ltr.labelMax + 1)
    val fu: Future[Int] = ltrmodelDAO.update(newModel.copy(model_data = Some(result), status =1, progress =1, finished_at = Some(new DateTime())))
    Await.ready(fu, scala.concurrent.duration.Duration.Inf)

    val jsonResponse = Json.obj(
      "mid" -> newModel.mid.get
    )
    Ok(jsonResponse)
  }


}



