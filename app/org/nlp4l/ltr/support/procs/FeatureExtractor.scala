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

import java.net.URLEncoder

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import org.nlp4l.ltr.support.actors.FeatureProgressReport
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.FeatureDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.LtrmodelDAO
import org.nlp4l.ltr.support.models.DocFeature
import org.nlp4l.ltr.support.models.FeatureExtractDTOs
import org.nlp4l.ltr.support.models.FeatureExtractResults
import org.nlp4l.ltr.support.models.LtrModels
import org.nlp4l.ltr.support.models.LtrModels._

import akka.actor.ActorRef
import dispatch._
import dispatch.Http
import dispatch.as
import dispatch.url
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import org.nlp4l.ltr.support.actors.FeatureExtractSetResultMsg




class FeatureExtractor(sender: ActorRef) extends FeatureProgressReport {
  
  def execute(dtos: FeatureExtractDTOs) = {  
    // Post the annotated docs and queries
    val s_req = url(dtos.featureExtractUrl).POST
          .setBody(Json.toJson(dtos).toString())
          .setHeader("Accept", "application/json")
          .setHeader("Content-Type", "application/json; charset=utf-8")
    s_req.subject.underlying { _.setBodyEncoding("UTF-8") }
    val s_f = Http(s_req OK as.String)
    val s_res = Await.result(s_f, scala.concurrent.duration.Duration.Inf)
    
    // Progress
    val s_res_json: JsValue = Json.parse(s_res)
    val procid = (s_res_json \ "results" \ "procid").as[String]
    val progressUrl = dtos.featureProgressUrl.replaceAll("\\$\\{procid\\}", URLEncoder.encode(procid, "UTF-8"))
    val p_req = url(progressUrl)
    
    var progressV: Int = 0
    do {
      val p_f = Http(p_req OK as.String)
      val p_res = Await.result(p_f, scala.concurrent.duration.Duration.Inf)
      val p_res_json: JsValue = Json.parse(p_res)
      progressV = (p_res_json \ "progress").as[Int]
      report(dtos.ltrid, sender, progressV)
      Thread.sleep(1000)
    } while (progressV < 100)
      
    // Save
    val retrieveUrl = dtos.featureRetrieveUrl.replaceAll("\\$\\{procid\\}", URLEncoder.encode(procid, "UTF-8"))
    val r_req = url(retrieveUrl)
    val r_f = Http(r_req OK as.String)
    val r_res = Await.result(r_f, scala.concurrent.duration.Duration.Inf)
    val r_result: FeatureExtractResults = Json.parse(r_res).validate[FeatureExtractResults].get
    sender ! FeatureExtractSetResultMsg(dtos.ltrid, r_result)
      
  }
}


// DEMO
object FeatureProgress {
  private var p: Int = 0
  def add(n: Int): Unit = {
    p = p + n
    if(p > 100) p = 100
  }
  def get(): Int = {
    p
  }
  def reset: Unit = {
    p = 0
  }
}


