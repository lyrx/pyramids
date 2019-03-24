package com.eternitas.lastwill



import org.scalajs.dom
import dom.crypto.{CryptoKey, CryptoKeyPair, HashAlgorithm, JsonWebKey, KeyFormat, KeyUsage, RsaHashedKeyAlgorithm, crypto}

import scala.concurrent.{ExecutionContext, Future}
import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array



 class Eternitas(val keysOpt:Option[CryptoKeyPair] =None) {



  def withKeys()(implicit ctx:ExecutionContext) = {
    if(keysOpt.isEmpty) Encrypt.generateKeys().map(key=>new Eternitas(keysOpt=Some(key)))
    else
      Future.successful(new Eternitas(keysOpt=Some(keysOpt.get)))

  }


  def exportKeyJWKPublic()(implicit ctx:ExecutionContext) = keysOpt.map(
    key=>Encrypt.eexportKey(key.publicKey)
  )

  def export()(implicit ctx:ExecutionContext) = keysOpt.map(
    key=>Encrypt
      .eexportKey(key.publicKey)
      .map(publicJw=>
        Encrypt.
          eexportKey(key.privateKey).
          map(privateJw=>js.Dynamic.literal(
            "private" ->privateJw,
            "public" -> publicJw)
          )).flatten
  ).
    getOrElse(Future{js.Dynamic.literal("empty" -> true)}).
    map(ee=>js.JSON.stringify(ee:js.Any,null:js.Array[js.Any],1:js.Any))

}


object Encrypt {
  val aKeyFormat =  KeyFormat.jwk
  val aAlgorithm =  RsaHashedKeyAlgorithm.`RSA-OAEP`(modulusLength = 4096,
    publicExponent = new Uint8Array( js.Array(1,0,1)),
    hash = HashAlgorithm.`SHA-256`)
    val usages = js.Array(
      KeyUsage.encrypt,
      KeyUsage.decrypt)
  val usageDecrypt = js.Array(
    KeyUsage.decrypt)
  val usageEncrypt = js.Array(
    KeyUsage.encrypt)



  def importJSON(eternitas: Eternitas,
                 json:js.Dynamic,
                 cb:(Eternitas)=>Unit)(
    implicit executionContext: ExecutionContext)=crypto.subtle.importKey(
       aKeyFormat,
       json.`public`.asInstanceOf[JsonWebKey],
       aAlgorithm,
       true,
       usageEncrypt
     ).toFuture.onComplete(_.map(aAny=>{
         val publicKey = aAny.asInstanceOf[CryptoKey]
         crypto.subtle.importKey(
           aKeyFormat,
           json.`private`.asInstanceOf[JsonWebKey],
           aAlgorithm,
           true,
           usageDecrypt
         ).toFuture.onComplete(_.map(aAny2=>{
           val privateKey = aAny2.asInstanceOf[CryptoKey]
           cb(new Eternitas(
           Some(js.Dictionary(
             "publicKey"->publicKey,
             "privateKey" -> privateKey
           ).asInstanceOf[CryptoKeyPair])))
         }))})

     )



  def eexportKey(key: CryptoKey)
                (implicit ctx:ExecutionContext) = crypto.subtle.
        exportKey(aKeyFormat,key).
        toFuture.map((a:Any)=>a.asInstanceOf[JsonWebKey])




  def generateKeys()(implicit ctx:ExecutionContext):Future[CryptoKeyPair]
  = crypto.subtle.generateKey(
    algorithm = aAlgorithm,
    extractable = true,
    keyUsages = usages).
    toFuture.map(_.asInstanceOf[CryptoKeyPair])




}
