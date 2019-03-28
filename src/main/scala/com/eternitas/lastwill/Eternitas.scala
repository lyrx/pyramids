package com.eternitas.lastwill

import com.eternitas.lastwill.axioss.Pinata
import com.eternitas.lastwill.cryptoo.{AsymCrypto, SymCrypto}
import org.scalajs.dom.crypto.{CryptoKey, CryptoKeyPair, JsonWebKey}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import js.Dynamic.{literal => l}

case class PinataAuth(api: String, secretApi: String)


case class EncryptedPin(dataHash:String,ivHash:String)

class Eternitas(
    val keyPairOpt: Option[CryptoKeyPair],
    val pinnataOpt: Option[PinataAuth],
    val keyOpt: Option[CryptoKey],
    val pins:Seq[EncryptedPin]
) {

  def withPin(encryptedPin: EncryptedPin) = new Eternitas(this.keyPairOpt,
    this.pinnataOpt,
    this.keyOpt,
    this.pins :+ encryptedPin )


  def withKeyPair()(implicit ctx: ExecutionContext) = {
    if (keyPairOpt.isEmpty)
      AsymCrypto
        .generateKeys()
        .map(
          key =>
            new Eternitas(
              keyPairOpt = Some(key),
              pinnataOpt = this.pinnataOpt,
              keyOpt = this.keyOpt,
              pins=this.pins))
    else
      Future.successful(this)
  }

  def withSymKey()(implicit ctx: ExecutionContext) = {
    if (keyOpt.isEmpty){
      val kf = SymCrypto
        .generateKey()
      kf.onComplete(t => {
       // t.failed.map(e=>println("Error generating symmetric key: " +e.getMessage))
       // t.map(key => println("Generated: " + key))
      })

        kf.map(
          key =>
            new Eternitas(
              keyPairOpt =this.keyPairOpt,
              pinnataOpt = this.pinnataOpt,
              keyOpt = Some(key),
              pins=this.pins
            ))}
    else
      Future.successful(this)
  }

  def withAllKeys()(implicit ctx: ExecutionContext) = withKeyPair().
    map(e=>e.withSymKey()).flatten



  def exportKeyPair()(implicit ctx: ExecutionContext):Future[js.Dynamic] = keyPairOpt
      .map(
        keyPair =>
          AsymCrypto
            .eexportKey(keyPair.publicKey)
            .map(publicJw =>
              AsymCrypto
                .eexportKey(keyPair.privateKey)
                .map(privateJw => l("private" -> privateJw, "public" -> publicJw))
                )
            .flatten
      ).getOrElse(Future{l()})


  def exportKey()()(implicit ctx: ExecutionContext):Future[js.Dynamic] = keyOpt
    .map(
      key =>
        SymCrypto.eexportKey(key).map(_.asInstanceOf[js.Dynamic]))
    .getOrElse(Future{l()})

  def exportPinata()()(implicit ctx: ExecutionContext):Future[js.Dynamic] = Future {pinnataOpt.
    map(p => new Pinata(p).export()).getOrElse(l("api" ->"","apisecret" ->""))}



  def export()(implicit ctx: ExecutionContext) ={
    Future.sequence(Seq(exportKeyPair(),exportKey(),exportPinata())).
      map(s => l("asym" -> s(0),"sym" -> s(1),"pinata" -> s(2)))
  }.map((aDynamic: js.Dynamic) =>
    js.JSON.stringify(aDynamic: js.Any, null: js.Array[js.Any], 1: js.Any))




  def expor_oldt()(implicit ctx: ExecutionContext) =
    keyPairOpt
      .map(
        key =>
          AsymCrypto
            .eexportKey(key.publicKey)
            .map(publicJw =>
              AsymCrypto
                .eexportKey(key.privateKey)
                .map(privateJw => {
                  pinnataOpt
                    .map(
                      p =>
                        l(
                          "pinata" -> new Pinata(p).export(),
                          "asym" -> l("private" -> privateJw,
                                      "public" -> publicJw)
                      ))
                    .getOrElse(l(
                      "pinata" -> l(),
                      "asym" -> l("private" -> privateJw, "public" -> publicJw)
                    ))
                }))
            .flatten
      )
      .getOrElse(Future {
        pinnataOpt
          .map(
            p =>
              l(
                "pinata" -> new Pinata(p).export()
            ))
          .getOrElse(l())
      })
      .map((aDynamic: js.Dynamic) =>
        js.JSON.stringify(aDynamic: js.Any, null: js.Array[js.Any], 1: js.Any))

}
