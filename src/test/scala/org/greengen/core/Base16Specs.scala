package org.greengen.core

import org.scalatest.flatspec.AnyFlatSpec

class Base16Specs extends AnyFlatSpec {

  "Base16 encoding-decoding" should "yield the original value" in {
    val bytes = Array[Byte](3,4,0,5,6,4,3,0,1,1,1,1,2,3,4,1,2,3)
    assert(Base16.decodeFrom(Base16.encodeToBase16(bytes)) === bytes)
  }

  "Base16 decoding-encoding" should "yield the encoding value" in {
    val bytes = Array[Byte](3,4,0,5,6,4,3,0,1,1,1,1,2,3,4,1,2,3)
    val b16 = Base16.encodeToBase16(bytes)
    assert(Base16.encodeToBase16(Base16.decodeFrom(b16)) === b16)
  }

  "decoding from a ill formatted string" should "yield an empty option" in {
    assert(Base16.decodeFrom("").isEmpty)
    assert(Base16.decodeFrom("1234567890abcdefg").isEmpty)
    assert(Base16.decodeFrom("1234567890 abcdef").isEmpty)
  }

  "All hexa-decimal characters" should "be valid" in {
    assert(Base16.decodeFrom("1234567890abcdef").nonEmpty)
  }

  "Hex string decoding" should "be case insensitive" in {
    val hexString = "1234567890abcdef"
    assert(Base16.decodeFrom(hexString).nonEmpty)
    assert(Base16.decodeFrom(hexString.toUpperCase).map(_.toList) === Base16.decodeFrom(hexString.toLowerCase).map(_.toList))
  }

  "MD5 decoding" should "work" in {
    println(Base16.decodeFrom("abf9a7ffe6bebf0db599ce6821ef09c0").map(_.toList))
    println(Base16.decodeFrom("35af4bf130805f0b86b1b13e49c8101e").map(_.toList))

    assert(Hash.md5("chris@green-gen.org").map(_.bytes).get ===
      Base16.decodeFrom("abf9a7ffe6bebf0db599ce6821ef09c0").map(_.toList).get)
    assert(Hash.md5("gogogo").map(_.bytes).get ===
      Base16.decodeFrom("35af4bf130805f0b86b1b13e49c8101e").map(_.toList).get)
  }

}
