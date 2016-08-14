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

/**
 * Job Dao
 */
package org.nlp4l.ltr.support.dao

import scala.concurrent.Future

import org.nlp4l.ltr.support.models.Feature

import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf



class FeatureDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val logger = Logger(this.getClass)
  
  class FeatureTable(tag: Tag) extends Table[Feature](tag, "features") {
    def fid = column[Int]("fid", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def ltrid = column[Int]("ltrid")
    def * = (fid.?, name, ltrid) <> (Feature.tupled, Feature.unapply)
  }

  val features = TableQuery[FeatureTable]

  def init = db.run(features.schema.create)

  def fetchAll(): Future[Seq[Feature]] = db.run(features.result)
  
  def count(): Int = features.length.asInstanceOf[Int]

  def get(fid: Int): Future[Feature] = {
    val query = features.filter(_.fid === fid)
    db.run(query.result.head)
  }

  def insert(Feature: Feature): Future[Feature] = {
    val featureWithId = (features returning features.map(_.fid) into ((Feature, id) => Feature.copy(fid=Some(id)))) += Feature
    db.run(featureWithId)
  }

  def update(Feature: Feature): Future[Int] = {
    val query = features.filter(_.fid === Feature.fid)
    db.run(query.update(Feature))
  }

  def delete(fid: Int): Future[Int] = {
    val query = features.filter(_.fid === fid)
    val res = db.run(query.delete)
    res
  }
  
  def fetch(sort: String, order: String, offset: Int = 0, size: Int = 10): Future[Seq[Feature]] = {
    sort match {
      case "fid" =>
        order match {
          case "asc" =>
            db.run(features.sortBy(_.fid.asc).drop(offset).take(size).result)
          case "desc" =>
            db.run(features.sortBy(_.fid.desc).drop(offset).take(size).result)
        }
    }
  }

  
  

}
