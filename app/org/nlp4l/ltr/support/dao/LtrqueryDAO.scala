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

import org.nlp4l.ltr.support.models.Ltrquery

import javax.inject.Inject
import play.api.Logger
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import slick.driver.JdbcProfile
import slick.lifted.ProvenShape.proveShapeOf



class LtrqueryDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {
  import driver.api._

  private val logger = Logger(this.getClass)
  
  class LtrqueryTable(tag: Tag) extends Table[Ltrquery](tag, "ltrqueries") {
    def qid = column[Int]("qid", O.PrimaryKey, O.AutoInc)
    def query = column[String]("query")
    def ltrid = column[Int]("ltrid")
    def checked_flg = column[Boolean]("checked_flg")
    def * = (qid.?, query, ltrid, checked_flg) <> (Ltrquery.tupled, Ltrquery.unapply)
  }

  val ltrqueries = TableQuery[LtrqueryTable]

  def init = db.run(ltrqueries.schema.create)

  def fetchAll(): Future[Seq[Ltrquery]] = db.run(ltrqueries.result)
  
  def count(): Int = ltrqueries.length.asInstanceOf[Int]

  def get(qid: Int): Future[Ltrquery] = {
    val query = ltrqueries.filter(_.qid === qid)
    db.run(query.result.head)
  }

  def insert(Ltrquery: Ltrquery): Future[Ltrquery] = {
    val LtrqueryWithId = (ltrqueries returning ltrqueries.map(_.qid) into ((Ltrquery, id) => Ltrquery.copy(qid=Some(id)))) += Ltrquery
    db.run(LtrqueryWithId)
  }

  def update(Ltrquery: Ltrquery): Future[Int] = {
    val query = ltrqueries.filter(_.qid === Ltrquery.qid)
    db.run(query.update(Ltrquery))
  }

  def delete(qid: Int): Future[Int] = {
    val query = ltrqueries.filter(_.qid === qid)
    val res = db.run(query.delete)
    res
  }
  
  def fetch(sort: String, order: String, offset: Int = 0, size: Int = 10): Future[Seq[Ltrquery]] = {
    sort match {
      case "qid" =>
        order match {
          case "asc" =>
            db.run(ltrqueries.sortBy(_.qid.asc).drop(offset).take(size).result)
          case "desc" =>
            db.run(ltrqueries.sortBy(_.qid.desc).drop(offset).take(size).result)
        }
    }
  }

  
  

}
