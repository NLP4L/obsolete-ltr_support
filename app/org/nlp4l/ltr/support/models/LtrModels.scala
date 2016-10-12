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

package org.nlp4l.ltr.support.models

import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json._
import play.api.libs.json.JsPath
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Reads
import play.api.libs.json.Writes


/**
 * Basic dto class for feature extraction
 */
case class FeatureExtractDTO(
  qid: Int,
  query: String,
  docs: List[String])

case class FeatureExtractDTOs(
  ltrid: Int,
  featureExtractUrl: String,
  featureProgressUrl: String,
  featureRetrieveUrl: String,
  dtos: List[FeatureExtractDTO]
)

case class FeatureExtractResult (
    fid: Int,
    qid: Int,
    docid: String,
    value: Float
)

case class FeatureExtractResults (
  procid: Int,
  progress: Long,
  results: List[FeatureExtractResult]
)

object LtrModels {

  implicit val fWFeatureExtractDTOWrites = new Writes[FeatureExtractDTO] {
    override def writes(d: FeatureExtractDTO): JsValue =
      Json.obj(
        "qid" -> d.qid,
        "query" -> d.query,
        "docs" -> d.docs)
  }

  implicit val fWFeatureExtractDTOsWrites = new Writes[FeatureExtractDTOs] {
    override def writes(d: FeatureExtractDTOs): JsValue =
      Json.obj(
        "queries" -> d.dtos)
  }
  
  

  implicit val fWFeatureExtractResultReads: Reads[FeatureExtractResult] = (
    (JsPath \ "fid").read[Int] and
    (JsPath \ "qid").read[Int] and
    (JsPath \ "docid").read[String] and
    (JsPath \ "value").read[Float])(FeatureExtractResult.apply _)
  
  implicit val fWFeatureExtractResultsReads: Reads[FeatureExtractResults] = (
    (JsPath \ "progress").read[Int] and
    (JsPath \ "procid").read[Long] and
    (JsPath \ "results").read[List[FeatureExtractResult]])(FeatureExtractResults.apply _)


  implicit val fWFeatureExtractResultWrites = new Writes[FeatureExtractResult] {
    override def writes(d: FeatureExtractResult): JsValue =
      Json.obj(
        "fid" -> d.qid,
        "qid" -> d.qid,
        "docid" -> d.docid,
        "value" -> d.value)
  }
  
  implicit val fWFeatureExtractResultsWrites = new Writes[FeatureExtractResults] {
    override def writes(d: FeatureExtractResults): JsValue =
      Json.obj(
        "progress" -> d.progress,
        "procid" -> d.procid,
        "results" -> d.results)
  }

}


