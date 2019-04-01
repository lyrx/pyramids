package com.eternitas.lastwill

import com.eternitas.lastwill.Buffers._

import com.eternitas.lastwill.axioss._
import com.eternitas.lastwill.cryptoo._
import com.eternitas.wizard.JQueryWrapper
import com.lyrx.eternitas.lastwill.LastWillStartup
import org.querki.jquery.{JQuery, JQueryEventObject}
import org.scalajs.dom
import org.scalajs.dom.raw._
import scala.concurrent.ExecutionContext
import scala.scalajs.js
import js.Dynamic.{literal => l}
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.typedarray.{ArrayBuffer, ArrayBufferView, Uint8Array}
import scala.util.Try
@js.native
@JSGlobal
class MyWindow extends dom.Window {
  val URL: URL = js.native

}
@js.native
trait URL extends js.Any {

  def createObjectURL(blob: Blob): String = js.native

}

object Actions {

  implicit class PimpedJQuery(jq: JQuery) extends PimpedQuery.PJQuery(jq) {


    def export(eternitas: Eternitas)(implicit ctx: ExecutionContext,
                                     $: JQueryWrapper,
                                     feedback: UserFeedback) = {
      eternitas
        .export()
        .onComplete(t =>
          if (t.isFailure) feedback.error("Export failed for keypair: " + t)
          else
            t.map((s: String) => {
              val blob: Blob =
                new Blob(js.Array(s), BlobPropertyBag("application/json"))
              val url = PimpedQuery.createObjectURL(blob)
              jq.attr("href", url)
            }))
      jq

    }


    def upLoad(eternitas: Eternitas)(implicit ctx: ExecutionContext,
                                     $: JQueryWrapper,
                                     feedback: UserFeedback) = onDrop(
      (file: File) =>
        eternitas.keyOpt.map(
          key =>
            new FileReader().onReadArrayBuffer(
              file,
              (arrayBuffer: ArrayBuffer) =>
                SymCrypto
                  .encrypt(key, arrayBuffer)
                  .onComplete((t: Try[SymEncryptionResult]) =>
                    onEncryptionResult(t, eternitas, file))
            ))
    ).onDragOverNothing()


    def showPinned(pinned: PinDataNative, eternitas: Eternitas)(
      implicit ctx: ExecutionContext,
      $: JQueryWrapper,
      feedback: UserFeedback) = {


      val el = $(
        s"<a href='#' download='${pinned.name.getOrElse("data.dat")}' " +
          s"  id='${pinned.hash.getOrElse(pinned.hashCode())}'  " +
          s"class='pinned'>${pinned.name.getOrElse("[UNNAMED]")}</a>")
      $("#data-display").append(el)
      el.click(
        (event: Event) => {
          event.preventDefault();
          event.stopPropagation()
          pinned.`hash`.map(aHash =>
            PimpedQuery.loadHashAsArrayBuffer(
              aHash,
              (encryptedData: ArrayBuffer) =>
                pinned.vc.map(avchash =>
                  PimpedQuery.loadHashAsArrayBuffer(
                    avchash,
                    (vc: ArrayBuffer) =>
                      eternitas.keyOpt.map(symKey => {
                        //feedback.log("Data to decrypt", encryptedData)
                        //feedback.log("IV for decrypt", vc)
                        val f = SymCrypto.decrypt(symKey, encryptedData, vc)
                        f.map((t: ArrayBuffer) => {
                          feedback.message("Decryption successfull")
                          val blob: Blob =
                            new Blob(js.Array[js.Any](t),
                              BlobPropertyBag(pinned.`type`.getOrElse("octet/stream").toString))
                          val url = PimpedQuery.createObjectURL(blob)
                          dom.window.open(url)
                        })
                        f.failed.map(e =>
                          feedback.error(
                            s"Decryption failed: ${e.getLocalizedMessage}"))
                      })
                  ))
            ))
        }
      )
    }

    def dataDisplay(eternitas: Eternitas)(implicit ctx: ExecutionContext,
                                          $: JQueryWrapper,
                                          feedback: UserFeedback) = {

      eternitas.pinDataOpt.map(
        aHash =>
          PimpedQuery.loadHashAsText(
            aHash,
            (data: String) =>
              js.JSON
                .parse(data.toString)
                .asInstanceOf[PinDataListNative]
                .data
                .map(realData =>
                  realData.foreach(pinned => showPinned(pinned, eternitas)))
          ))
    }


    private def onEncryptionResult(t: Try[SymEncryptionResult],
                                   eternitas: Eternitas,
                                   file: File)(
                                    implicit feedback: UserFeedback,
                                    $: JQueryWrapper,
                                    executionContext: ExecutionContext) = {

      t.failed.map(thr =>
        feedback.error(s"Encryption failed: ${thr.getMessage()}"))
      t.map((symEncryptionResult: SymEncryptionResult) => {
        feedback.log(s"Encrypted", symEncryptionResult.result)
        feedback.log(s"IV", symEncryptionResult.iv)
        eternitas.pinataAuth.map(p =>
          withPinata(eternitas, file, symEncryptionResult, p))
      })

    }

    def withPinata(eternitas: Eternitas,
                   file: File,
                   symEncryptionResult: SymEncryptionResult,
                   p: PinataAuth)(implicit feedback: UserFeedback,
                                  executionContext: ExecutionContext,
                                  $: JQueryWrapper): AxiosImpl = {

      feedback.message("Start pinning, please be very patient!")
      new Pinata(p)
        .pinFileToIPFS(symEncryptionResult.result, PinataMetaData(file))
        .`then`((axiosResponse) =>
          pinataUpload(file, eternitas, symEncryptionResult, p, axiosResponse))
        .`catch`((error) => feedback.message(s"Error pinning hash: ${error}"))

    }

    def pinataUpload(file: File,
                     eternitas: Eternitas,
                     symEncryptionResult: SymEncryptionResult,
                     p: PinataAuth,
                     axiosResponse: AxiosResponse)(
                      implicit $: JQueryWrapper,
                      feedback: UserFeedback): AxiosImpl = {

      new Pinata(p)
        .pinFileToIPFS(symEncryptionResult.iv,
          PinataMetaData(
            axiosResponse
              .asInstanceOf[PinataPinResponse]
              .data))
        .`then`((axiosResponse2) =>
          handlePinResult(file, eternitas, axiosResponse, axiosResponse2))
        .`catch`((error) => feedback.message(s"Pinning error: ${error}"))

    }

    private def handlePinResult(file: File,
                                eternitas: Eternitas,
                                axiosResponse: AxiosResponse,
                                axiosResponse2: AxiosResponse)(
                                 implicit $: JQueryWrapper,
                                 userFeedback: UserFeedback) = {

      val dataHash = axiosResponse.asInstanceOf[PinataPinResponse].data.IpfsHash
      val ivHash = axiosResponse2.asInstanceOf[PinataPinResponse].data.IpfsHash

      userFeedback.logString(s"Data uploaded: ${dataHash}")
      userFeedback.logString(s"IV uploaded: ${ivHash}")

      pinDataList(file, dataHash, ivHash, eternitas, e => {
        LastWillStartup.init(e)
        userFeedback.message(s"Your data is encrypted and stored!")
      })

    }

    def pinDataList(file: File,
                    dataHash: String,
                    ivHash: String,
                    eternitas: Eternitas,
                    cb: (Eternitas) => js.Any)(implicit $: JQueryWrapper,
                                               userFeedback: UserFeedback) = {
      import WalletNative._;

      def mLoadPinData(cb: (PinDataListNative) => Unit) =
        if (eternitas.pinDataOpt.isDefined)
          eternitas.pinDataOpt.map((aHash: String) =>
            PimpedQuery.loadHashAsText(aHash, (s: String) => {
              cb(js.JSON.parse(s).asInstanceOf[PinDataListNative])
            }))
        else
          cb(l("data" -> js.Array()).asInstanceOf[PinDataListNative])

      mLoadPinData((pinDataNative) => {
        val pinString = Eternitas.stringify(
          pinDataNative.withPinData(
            l("hash" -> dataHash,
              "vc" -> ivHash,
              "name" -> file.name,
              "type" -> file.`type`).asInstanceOf[PinDataNative]))

        eternitas.pinataAuth.map(auth => {
          // userFeedback.logString("Pinning: " + pinString)
          new Pinata(auth)
            .pinFileToIPFS(
              pinString.toArrayBuffer(),
              PinataMetaData(name = Some("Eternitas-Data"),
                size = None,
                `type` = Some("application/json"))
            )
            .`then`((r: AxiosResponse) => {
              val eternitasHash = r.asInstanceOf[PinataPinResponse].data.IpfsHash
              userFeedback.logString(s"Pinned Eternitas-Data: ${eternitasHash}")
              cb(eternitas.withPinDataHash(eternitasHash))
            })
            .`catch`((e: AxiosError) => {
              userFeedback.error(s"Error pinning eternitas data list: ${e}")
              cb(eternitas)
            })
        })
      })
    }


  }

}

