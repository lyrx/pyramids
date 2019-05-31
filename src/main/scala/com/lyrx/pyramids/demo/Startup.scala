package com.lyrx.pyramids.demo

import com.lyrx.pyramids.frontend.UserFeedback
import com.lyrx.pyramids.ipfs.CanIpfs
import com.lyrx.pyramids.jszip.JJSZip
import com.lyrx.pyramids.keyhandling.DragAndDrop
import com.lyrx.pyramids.{Pyramid, PyramidConfig}
import org.scalajs.dom.{Event, File, document}
import typings.jqueryLib.{JQuery, JQueryEventObject, jqueryMod => $}
import typings.jszipLib.jszipMod.JSZip

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Startup extends DragAndDrop with UserFeedback {
  implicit val ec = ExecutionContext.global

  override def msgField[T](): JQuery[T] = {
    $("#message")
  }

  override def timeField[T](): JQuery[T] = $("#time")

  def main(args: Array[String]): Unit =
    document.addEventListener("DOMContentLoaded", (e: Event) => startup())

  def startup() = {

    message("Generating keys ...")
    Pyramid()
      .generateKeys()
      .map(p => ipfsInit(p.pyramidConfig))

  }

  def ipfsInit(pyramidConfig: PyramidConfig)(
    implicit executionContext: ExecutionContext) = {
    message("Connecting IPFS network ...")
    val f =
      new Pyramid(
        pyramidConfig
      ).initIpfsAndPublishPublicKeys()

    f.failed.map(thr => error(s"Initialization Error: ${thr.getMessage}"))
    f.map((p: Pyramid) => init(p.pyramidConfig))

  }

  def handleWithIpfs(f: Future[PyramidConfig], msgOpt: Option[String] = None) = {
    msgOpt.map(message(_))
    f.onComplete(t => t.failed.map(thr => error(thr.getMessage)))
    f.map(config => ipfsInit(config))
  }
  def handle(f: Future[PyramidConfig], msgOpt: Option[String] = None) = {
    msgOpt.map(message(_))
    f.onComplete(t => t.failed.map(thr => error(thr.getMessage)))
    f.map(config => init(config))
  }

  def init(pyramidConfig: PyramidConfig)(
    implicit executionContext: ExecutionContext): Future[PyramidConfig] = {

    val pyramid = new Pyramid(pyramidConfig)

    //show message and error
    pyramidConfig.messages.messageOpt.map(s => message(s))
    pyramidConfig.messages.errorOpt.map(s => error(s))

    // prevent default for drag and droo
    onDragOverNothing($(".front-page").off())
      .on("drop", (e: JQueryEventObject) => e.preventDefault())

    //Download/upload wallet:
    pyramid
      .downloadWallet($("#logo").off())
      .map(
        (q2: JQuery[_]) =>
          onDrop(q2,
            (f) =>
              handleWithIpfs(pyramid.uploadWallet(f),
                Some(s"Importing keys from ${f.name}"))))

   onDrop($("#drop_zone").off(),
     (f)=>handle(uploadMe(pyramid, f)))

    Future {pyramidConfig}

  }

   def uploadMe(pyramid: Pyramid, f: File) = {
     message("Uploading ...")
     pyramid
       .zipEncrypt(f).
       flatMap(_.dump()).
       flatMap(b=>pyramid.bufferToIpfs(b)).
       map(os=>os.map(s=>pyramid.
         pyramidConfig.
         msg(s"Uploaded: ${s}")).
         getOrElse(pyramid.pyramidConfig))
   }


}