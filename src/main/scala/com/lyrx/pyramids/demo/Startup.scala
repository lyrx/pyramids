package com.lyrx.pyramids.demo

import com.lyrx.pyramids.keyhandling.DragAndDrop
import com.lyrx.pyramids.frontend.UserFeedback
import com.lyrx.pyramids.jszip.JSZip
import com.lyrx.pyramids.{Pyramid, PyramidConfig}
import org.scalajs.dom.{Event, document}
import org.scalajs.jquery.{JQuery, JQueryEventObject, jQuery => $}

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

object Startup{
  def main(args: Array[String]): Unit = document.addEventListener(
    "DOMContentLoaded",
    (e: Event) =>new Startup().startup())

}

class Startup extends DragAndDrop with UserFeedback{
  implicit val ec = ExecutionContext.global

  override def msgField():JQuery= $("#message")
  def timeField():JQuery = $("#time")


  //test suggested by Alex.


  def startup()={


    message("Generating keys ...")
    Pyramid()
      .generateKeys().map(p=>ipfsInit(p.pyramidConfig))


  }


  def ipfsInit(pyramidConfig: PyramidConfig)(
    implicit executionContext: ExecutionContext)= {
    message("Connecting IPFS network ...")
    val f =
    new Pyramid(
      pyramidConfig
    ).initIpfsAndPublishPublicKeys()

    f.failed.map(thr => error(s"Initialization Error: ${thr.getMessage}"))
    f.map((p:Pyramid) => init(p.pyramidConfig))

  }

  def handle(f: Future[PyramidConfig],msgOpt:Option[String]=None) = {
    msgOpt.map(message(_))
    f.onComplete(t => t.failed.map(thr => error(thr.getMessage)))
    f.map(config => ipfsInit(config))
  }

  def click(selector: String, c: (Event) => Future[PyramidConfig]) =
    $(selector).off().click((e: Event) => handle(c(e)))


  def init(pyramidConfig: PyramidConfig)(
      implicit executionContext: ExecutionContext): Future[PyramidConfig] = {

    val pyramid = new Pyramid(pyramidConfig)

    //show message and error
    pyramidConfig.messages.messageOpt.map(s => message(s))
    pyramidConfig.messages.errorOpt.map(s => error(s))

    // prevent default for drag and droo
    onDragOverNothing($(".front-page").off()).on("drop", (e: Event) => e.preventDefault())

    //Download/upload wallet:
    pyramid
      .downloadWallet($("#logo").off())
      .map((q2: JQuery) => onDrop(q2, (f) => handle(
        pyramid.uploadWallet(f),
        Some(s"Importing keys from ${f.name}"))))

    Future {
      pyramidConfig
    }
  }


}
