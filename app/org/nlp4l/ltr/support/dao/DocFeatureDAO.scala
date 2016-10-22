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

import org.nlp4l.ltr.support.models.DocFeature

import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf



class DocFeatureDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val logger = Logger(this.getClass)
  
  class DocFeatureTable(tag: Tag) extends Table[DocFeature](tag, "docfeatures") {
    def id = column[Option[Int]]("id", O.PrimaryKey, O.AutoInc)
    def qid = column[Int]("qid")
    def fid = column[Int]("fid")
    def docid = column[String]("docid")
    def value = column[Float]("value")
    def * = (id, fid, qid, docid, value) <> (DocFeature.tupled, DocFeature.unapply)
    def uk = index("uk_docfeatures", (qid, fid, docid), unique = true)
  }

  val docfeatures = TableQuery[DocFeatureTable]

  def init = db.run(docfeatures.schema.create)

  def fetchAll(): Future[Seq[DocFeature]] = db.run(docfeatures.result)
  
  def count(): Int = docfeatures.length.asInstanceOf[Int]

  def get(id: Int): Future[DocFeature] = {
    val query = docfeatures.filter(_.id === id)
    db.run(query.result.head)
  }

  def insert(docfeature: DocFeature): Future[DocFeature] = {
    val dataWithId = (docfeatures returning docfeatures.map(_.id) into ((df, id) => df.copy(id=id))) += docfeature
    db.run(dataWithId)
  }

  def update(DocFeature: DocFeature): Future[Int] = {
    val query = docfeatures.filter(_.qid === DocFeature.qid).filter(_.fid === DocFeature.fid).filter(_.docid === DocFeature.docid)
    db.run(query.update(DocFeature))
  }

  def delete(id: Int): Future[Int] = {
    val query = docfeatures.filter(_.id === id)
    val res = db.run(query.delete)
    res
  }
  
  def deleteByQuid(qid: Int): Future[Int] = {
    val query = docfeatures.filter(_.qid === qid)
    val res = db.run(query.delete)
    res
  }
  
  def fetch(sort: String, order: String, offset: Int = 0, size: Int = 10): Future[Seq[DocFeature]] = {
    sort match {
      case "qid" =>
        order match {
          case "asc" =>
            db.run(docfeatures.sortBy(_.qid.asc).drop(offset).take(size).result)
          case "desc" =>
            db.run(docfeatures.sortBy(_.qid.desc).drop(offset).take(size).result)
        }
    }
  }

  def fetchByFids(fids: Seq[Int]): Future[Seq[DocFeature]] = {
    val query = docfeatures.filter(_.fid inSetBind fids)
    val res = db.run(query.result)
    res
  }

}
