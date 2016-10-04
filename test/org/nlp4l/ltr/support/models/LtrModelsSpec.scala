package org.nlp4l.ltr.support.models

import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.junit.runner.RunWith

import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Writes

import org.nlp4l.ltr.support.models.LtrModels._

@RunWith(classOf[JUnitRunner])
class LtrModelsSpec extends Specification {

  "LtrModels" should {
    val dto1 = FeatureExtractDTO(1, "あああ", List("いいい", "ううう"))
    val dto2 = FeatureExtractDTO(2, "ううう", List("えええ", "ううう"))
    val dtolist: List[FeatureExtractDTO] = List(dto1, dto2)
    val expected1 = """{"qid":1,"query":"あああ","docs":["いいい","ううう"]}"""
    val expected2 = """{"queries":[{"qid":1,"query":"あああ","docs":["いいい","ううう"]},{"qid":2,"query":"ううう","docs":["えええ","ううう"]}]}"""
    
    "fWFeatureExtractDTOWrites return a dto json" in {
      val result = Json.toJson(dto1).toString();
      result == expected1
    }
    
    "fWFeatureExtractDTOListWrites return a json list of some dtos" in {
      val result = Json.toJson(dtolist).toString();
      result == expected2
    }

  }
  
}