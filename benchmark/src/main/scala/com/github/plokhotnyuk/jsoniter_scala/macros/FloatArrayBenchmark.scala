package com.github.plokhotnyuk.jsoniter_scala.macros

import java.nio.charset.StandardCharsets._

import com.github.plokhotnyuk.jsoniter_scala.core._
import com.github.plokhotnyuk.jsoniter_scala.macros.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.macros.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.macros.JsoniterCodecs._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.Benchmark
import play.api.libs.json.Json

class FloatArrayBenchmark extends CommonParams {
  val obj: Array[Float] = (1 to 512)
    .map(i => (((i * 1498724053) / Math.pow(10, i % 10)).toInt * Math.pow(10, (i % 32) - 16)).toFloat).toArray
  val jsonString: String = obj.mkString("[", ",", "]")
  val jsonBytes: Array[Byte] = jsonString.getBytes

  @Benchmark
  def readCirce(): Array[Float] = decode[Array[Float]](new String(jsonBytes, UTF_8)).fold(throw _, x => x)

  @Benchmark
  def readJackson(): Array[Float] = jacksonMapper.readValue[Array[Float]](jsonBytes)

  @Benchmark
  def readJsoniter(): Array[Float] = JsonReader.read(floatArrayCodec, jsonBytes)

  @Benchmark
  def readPlay(): Array[Float] = Json.parse(jsonBytes).as[Array[Float]]

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeJackson(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniter(): Array[Byte] = JsonWriter.write(floatArrayCodec, obj)

  @Benchmark
  def writeJsoniterPrealloc(): Int = JsonWriter.write(floatArrayCodec, obj, preallocatedBuf, 0)
/* FIXME: Play-JSON serialize double values instead of float
  @Benchmark
  def writePlay(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
}