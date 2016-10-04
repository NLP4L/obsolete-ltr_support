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

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

/**
 * Ltrconfig Table
 */
case class FeatureExtractDTO (
    qid: Int, 
    query: String, 
    docs: List[String]
)



object LtrModels {

  implicit val fWFeatureExtractDTOWrites = new Writes[FeatureExtractDTO] {
    override def writes(d: FeatureExtractDTO): JsValue =
      Json.obj(
        "qid" -> d.qid,
        "query" -> d.query,
        "docs" -> d.docs
      )
  }
  
  implicit val fWFeatureExtractDTOListWrites = new Writes[List[FeatureExtractDTO]] {
    override def writes(d: List[FeatureExtractDTO]): JsValue =
      Json.obj(
        "queries" -> d
      )
  }
  
}
