package com.lyrx.pyramids.keyhandling

import com.lyrx.pyramids.pcrypto.SymetricCrypto
import com.lyrx.pyramids.{Pyramid, PyramidConfig}

import org.scalajs.dom.raw.{File,FileReader}

import scala.concurrent.{ExecutionContext, Future}

trait Encryption extends  SymetricCrypto  {

  val pyramidConfig: PyramidConfig

  def symEncrypt()(implicit ctx:ExecutionContext) =  pyramidConfig.
        symKeyOpt.map(symKey=> encryptDir(symKey,pyramidConfig.distributedDir)).
    map(_.map(d=>new Pyramid(
      pyramidConfig.copy(distributedDir = d).
        msg("Your data has been encryted, oh Pharao!")
    ))).
    getOrElse(Future{new Pyramid(pyramidConfig.msg("Oh pharao, we have not found your encryption key!"))})


  def encryptFile(f:File) (implicit ctx:ExecutionContext)= pyramidConfig.
    symKeyOpt.
    map(k=>symEncryptFile(k,f).map(Some(_))).
    getOrElse(Future{None})

}
