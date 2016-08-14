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

import org.nlp4l.ltr.support.models.Ltrsupervise

import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf



class LtrsuperviseDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val logger = Logger(this.getClass)
  
  class LtrsuperviseTable(tag: Tag) extends Table[Ltrsupervise](tag, "ltrsupervises") {
    def qid = column[Int]("qid")
    def docid = column[String]("docid")
    def label = column[Int]("label")
    def * = (qid, docid, label) <> (Ltrsupervise.tupled, Ltrsupervise.unapply)
    def pk = primaryKey("pk_ltrsupervises", (qid, docid))
  }

  val ltrsupervises = TableQuery[LtrsuperviseTable]

  def init = db.run(ltrsupervises.schema.create)

  def fetchAll(): Future[Seq[Ltrsupervise]] = db.run(ltrsupervises.result)
  
  def count(): Int = ltrsupervises.length.asInstanceOf[Int]

  def get(qid: Int, docid: String): Future[Ltrsupervise] = {
    val query = ltrsupervises.filter(_.qid === qid).filter(_.docid === docid)
    db.run(query.result.head)
  }

  def insert(ltrsupervise: Ltrsupervise): Future[Ltrsupervise] = {
    val LtrsuperviseWithId = (ltrsupervises returning ltrsupervises.map(_.qid) into ((ltrsupervise, id) => ltrsupervise.copy(qid=id))) += ltrsupervise
    db.run(LtrsuperviseWithId)
  }

  def update(Ltrsupervise: Ltrsupervise): Future[Int] = {
    val query = ltrsupervises.filter(_.qid === Ltrsupervise.qid).filter(_.docid === Ltrsupervise.docid)
    db.run(query.update(Ltrsupervise))
  }

  def delete(qid: Int, docid: String): Future[Int] = {
    val query = ltrsupervises.filter(_.qid === qid).filter(_.docid === docid)
    val res = db.run(query.delete)
    res
  }
  
  def fetch(sort: String, order: String, offset: Int = 0, size: Int = 10): Future[Seq[Ltrsupervise]] = {
    sort match {
      case "qid" =>
        order match {
          case "asc" =>
            db.run(ltrsupervises.sortBy(_.qid.asc).drop(offset).take(size).result)
          case "desc" =>
            db.run(ltrsupervises.sortBy(_.qid.desc).drop(offset).take(size).result)
        }
    }
  }

  
  

}
