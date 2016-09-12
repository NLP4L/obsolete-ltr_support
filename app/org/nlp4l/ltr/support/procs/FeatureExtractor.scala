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


import akka.actor.Actor
import akka.actor.Props
import javax.inject.Inject
import org.nlp4l.ltr.support.dao.LtrmodelDAO
import org.nlp4l.ltr.support.dao.LtrconfigDAO
import org.nlp4l.ltr.support.dao.DocFeatureDAO
import org.nlp4l.ltr.support.dao.LtrannotationDAO
import org.nlp4l.ltr.support.dao.LtrqueryDAO
import org.nlp4l.ltr.support.dao.FeatureDAO
import scala.util.Random
import akka.actor.ActorRef
import play.api.Logger
import org.nlp4l.ltr.support.actors.FeatureProgressReport



class FeatureExtractor(sender: ActorRef) extends FeatureProgressReport {
  override def execute(ltrid: Int) = {
    // TODO feature extraction
    
    // DEMO
    FeatureProgress.reset
    var progressV: Int = 0
    do {
      FeatureProgress.add(Random.nextInt(10))
      progressV = FeatureProgress.get()
      report(ltrid, sender, progressV)
      Thread.sleep(1000)
    } while (progressV < 100)
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


