package org.nlp4l.ltr.support.models

import org.junit.runner.RunWith
import org.nlp4l.ltr.support.models.LtrModels._
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import play.api.libs.json.JsValue
import play.api.libs.json.Json.toJsFieldJsValueWrapper
import play.api.libs.json.Json


@RunWith(classOf[JUnitRunner])
class LtrModelsSpec extends Specification {

  "LtrModels" should {
    val dto1 = FeatureExtractDTO(1, "あああ", List("いいい", "ううう"))
    val dto2 = FeatureExtractDTO(2, "ううう", List("えええ", "ううう"))
    
    val dtolist: List[FeatureExtractDTO] = List(dto1, dto2)
    val dtos: FeatureExtractDTOs = FeatureExtractDTOs(1, "aaa", "bbb", "ccc", dtolist)
    val expected1 = """{"qid":1,"query":"あああ","docs":["いいい","ううう"]}"""
    val expected2 = """{"queries":[{"qid":1,"query":"あああ","docs":["いいい","ううう"]},{"qid":2,"query":"ううう","docs":["えええ","ううう"]}]}"""
    
    "fWFeatureExtractDTOWrites return a dto json" in {
      val result = Json.toJson(dto1).toString();
      result == expected1
    }
    
    "fWFeatureExtractDTOsWrites return a json list of some dtos" in {
      val result = Json.toJson(dtos).toString();
      result == expected2
    }
    
    
    val json3_1 = """{"fid":1,"qid":2,"docid":"あああ","value":12.3}"""
    val expected3_1 = FeatureExtractResult(1, 2, "あああ", 12.3f)
    val json3_2 = """{"fid":11,"qid":22,"docid":"いいい","value":112.3}"""
    val expected3_2 = FeatureExtractResult(11, 22, "いいい", 112.3f)
    
    val json4 = """{"progress": 20, "procid": 222, "results": ["""+json3_1+""","""+json3_2+"""]}"""
    val expected4 = FeatureExtractResults(20, 222, List(expected3_1, expected3_2))
    
    "fWFeatureExtractResultReads return a FeatureExtractResult object from json data" in {
      val result = Json.parse(json3_1).validate[FeatureExtractResult].get
      result == expected3_1
    }
    
    "fWFeatureExtractResultsReads return the FeatureExtractResult object list from json data" in {
      val result = Json.parse(json4).validate[FeatureExtractResults].get
      result == expected4
    }
    
    "fWFeatureExtractResultWrites return a dto json" in {
      val result = Json.toJson(expected3_1).toString();
      result == json3_1
    }

  }
  
}