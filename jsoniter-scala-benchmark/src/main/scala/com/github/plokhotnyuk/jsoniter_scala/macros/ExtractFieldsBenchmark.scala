package com.github.plokhotnyuk.jsoniter_scala.macros

import argonaut.Argonaut._
//import java.io.{File, FileInputStream, FileOutputStream}
//import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets._
//import java.nio.file.StandardOpenOption._
//import java.nio.file.{Path, Paths}
import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.AVSystemCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import com.github.plokhotnyuk.jsoniter_scala.macros.PlayJsonFormats._
import com.github.plokhotnyuk.jsoniter_scala.macros.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.macros.HashCodeCollider.zeroHashCodeStrings
import io.circe.generic.auto._
import io.circe.parser._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
import play.api.libs.json.Json
import upickle.default._

case class ExtractFields(s: String, i: Int)

class ExtractFieldsBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000"/*, "1000000" FIXME: uncomment when patch for this issue will be released: https://github.com/playframework/play-json/issues/186 */))
  var size: Int = 10
  @Param(Array("""[2.1,""]"""))
  var value = """[2.1,""]"""
  var obj: ExtractFields = ExtractFields("s", 1)
  var jsonString: String = _
  var jsonBytes: Array[Byte] = _
//  val tmpFilePath: Path = Paths.get(File.createTempFile("extract-fields-benchmark", ".json").getAbsolutePath)

  @Setup
  def setup(): Unit = {
    jsonString = zeroHashCodeStrings.take(size).mkString("""{"s":"s","""", s"""":$value,"""", s"""":$value,"i":1}""")
    jsonBytes = jsonString.getBytes(UTF_8)
/*
    val fos = new FileOutputStream(tmpFilePath.toFile)
    try fos.write(jsonBytes)
    finally fos.close()
*/
  }

  @Benchmark
  def readArgonaut(): ExtractFields = {
    import com.github.plokhotnyuk.jsoniter_scala.macros.ArgonautCodecs._
    new String(jsonBytes, UTF_8).decode[ExtractFields].getOrElse(throw new RuntimeException)
  }

  @Benchmark
  def readAVSystemGenCodec(): ExtractFields = JsonStringInput.read[ExtractFields](new String(jsonBytes, UTF_8))

  @Benchmark
  def readCirce(): ExtractFields = decode[ExtractFields](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readDslJsonJava(): ExtractFields = decodeDslJson[ExtractFields](jsonBytes)

  @Benchmark
  def readJacksonScala(): ExtractFields = jacksonMapper.readValue[ExtractFields](jsonBytes)

  @Benchmark
  def readJsoniterScala(): ExtractFields = readFromArray[ExtractFields](jsonBytes)
/*
  @Benchmark
  def readJsoniterScalaIO(): ExtractFields = {
    val fis = new FileInputStream(tmpFilePath.toFile)
    try readFromStream[ExtractFields](fis)
    finally fis.close()
  }

  //FIXME: lot of warnings like: [warning][os,thread] Failed to start thread - pthread_create failed (EAGAIN) for attributes: stacksize: 1024k, guardsize: 4k, detached.
  //Read about mmap of files: http://www.mapdb.org/blog/mmap_files_alloc_and_jvm_crash/
  @Benchmark
  def readJsoniterScalaNIO(): ExtractFields = {
    val fc = FileChannel.open(tmpFilePath, READ)
    try readFromByteBuffer[ExtractFields](fc.map(FileChannel.MapMode.READ_ONLY, 0L, fc.size))
    finally fc.close()
  }
*/
  @Benchmark
  def readPlayJson(): ExtractFields = Json.parse(jsonBytes).as[ExtractFields](extractFieldsFormat)

  @Benchmark
  def readUPickle(): ExtractFields = read[ExtractFields](jsonBytes)
}