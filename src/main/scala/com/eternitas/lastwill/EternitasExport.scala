package com.eternitas.lastwill

import org.scalajs.dom.crypto.JsonWebKey

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

@js.native
@JSGlobal
class EternitasExport(
                       privateKey:js.UndefOr[JsonWebKey],
                       publicKey:js.UndefOr[JsonWebKey]
                     ) extends js.Object {

}
