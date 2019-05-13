package com.lyrx.pyramids.PyramidCrypt

import org.scalajs.dom.crypto.{CryptoKey, CryptoKeyPair, HashAlgorithm, JsonWebKey, KeyAlgorithmIdentifier, KeyFormat, KeyUsage, RsaHashedKeyAlgorithm, crypto}

import scala.scalajs.js
import scala.scalajs.js.typedarray.Uint8Array
import js.Dynamic.{literal => l}
import scala.concurrent.{ExecutionContext, Future}
trait AsymetricCrypto extends Crypto {
  val aKeyFormat =  KeyFormat.jwk
  val aHashAlgorithm =  RsaHashedKeyAlgorithm.`RSA-OAEP`(modulusLength = 4096,
    publicExponent = new Uint8Array( js.Array(1,0,1)),
    hash = HashAlgorithm.`SHA-256`)

  val aSignAlgorithm = l(
    "name" ->"ECDSA",
    "namedCurve" -> "P-384"
  ).asInstanceOf[KeyAlgorithmIdentifier]

  val aAlgorithm:KeyAlgorithmIdentifier = js.Dynamic.
    literal("name" -> "RSA-OAEP").
    asInstanceOf[KeyAlgorithmIdentifier]



  val usageDecrypt=js.Array(KeyUsage.decrypt)
  val usageEncrypt=js.Array(KeyUsage.encrypt)
  val usageSign=js.Array(KeyUsage.sign)
  val usageVerify=js.Array(KeyUsage.verify)



  def generateASymetricEncryptionKeys()(implicit ctx:ExecutionContext):Future[CryptoKeyPair]
  = generateKeysFor( js.Array(KeyUsage.encrypt, KeyUsage.decrypt),aHashAlgorithm)


  def generateSignKeys()(implicit ctx:ExecutionContext):Future[CryptoKeyPair]
  = generateKeysFor(js.Array(KeyUsage.sign, KeyUsage.verify), aSignAlgorithm)




  private def generateKeysFor(aUsage:js.Array[KeyUsage],alg:KeyAlgorithmIdentifier)
                     (implicit ctx:ExecutionContext):Future[CryptoKeyPair]
  = crypto.subtle.generateKey(
    algorithm = alg
    ,
    extractable = true,
    keyUsages = aUsage).
    toFuture.map(_.asInstanceOf[CryptoKeyPair])


  def importKeyPair(
                     keypairNative: KeypairNative,
                     privateUsage:js.Array[KeyUsage],
                     publicUsage:js.Array[KeyUsage]
                   )(
                     implicit executionContext: ExecutionContext)=keypairNative.
    `private`.map(aJSPrivateKey=>
    importKey(
      aJSPrivateKey, privateUsage)).
    getOrElse(Future{None}).
    flatMap( (privateKeyOpt:Option[CryptoKey])=>keypairNative.
      `public`.map(
      aJSPublicKey=>
      importKey(aJSPublicKey, publicUsage).
        map(publicKeyOpt=>
        toKeyPair(privateKeyOpt,publicKeyOpt))).
      getOrElse(Future{toKeyPair(privateKeyOpt,None)}))




  def toKeyPair(privateKey: Option[CryptoKey], publicKey: Option[CryptoKey]) = js.Dictionary(
    "publicKey" -> publicKey.getOrElse(null),
    "privateKey" -> privateKey.getOrElse(null)
  ).asInstanceOf[CryptoKeyPair]



  private def importKey(apublicKey: JsonWebKey,usages: js.Array[KeyUsage])(
    implicit executionContext: ExecutionContext) = crypto.subtle.importKey(
      aKeyFormat,
      apublicKey,
      aHashAlgorithm,
      true,
      usages).toFuture.
      map(k=>Some(k.asInstanceOf[CryptoKey]))

}
