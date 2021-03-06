package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.math.MathContext
import java.nio.charset.StandardCharsets.UTF_8

import com.avsystem.commons.serialization.json._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.CirceEncodersDecoders._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.DslPlatformJson._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JacksonSerDesers._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.benchmark.UPickleReaderWriters._
import com.github.plokhotnyuk.jsoniter_scala.core._
import io.circe.parser._
import io.circe.syntax._
import org.openjdk.jmh.annotations.{Benchmark, Param, Setup}
//import play.api.libs.json.Json

class BigDecimalBenchmark extends CommonParams {
  @Param(Array("1", "10", "100", "1000", "10000", "100000", "1000000"))
  var size: Int = 300
  var jsonBytes: Array[Byte] = _
  var jsonString: String = _
  var sourceObj: BigDecimal = _
  var preallocatedBuf: Array[Byte] = _

  @Setup
  def setup(): Unit = {
    jsonBytes = (1 to size).map(i => ((i % 10) + '0').toByte).toArray
    jsonString = new String(jsonBytes)
    sourceObj = BigDecimal(jsonString)
    preallocatedBuf = new Array(jsonBytes.length + 100/*to avoid possible out of bounds error*/)
  }

  private def obj: BigDecimal = // to avoid internal caching of the string representation
    BigDecimal(sourceObj.bigDecimal.unscaledValue(), sourceObj.bigDecimal.scale(), sourceObj.mc)

  @Benchmark
  def readAVSystemGenCodec(): BigDecimal = JsonStringInput.read[BigDecimal](new String(jsonBytes, UTF_8),
    JsonOptions.Default.copy(mathContext = MathContext.UNLIMITED /*WARNING: don't do this for open-system*/))

  @Benchmark
  def readCirce(): BigDecimal = decode[BigDecimal](new String(jsonBytes, UTF_8)).fold(throw _, identity)

  @Benchmark
  def readDslJsonScala(): BigDecimal = dslJsonDecode[BigDecimal](jsonBytes)

  @Benchmark
  def readJacksonScala(): BigDecimal = jacksonMapper.readValue[BigDecimal](jsonBytes)

  @Benchmark
  def readJsoniterScala(): BigDecimal = readFromArray[BigDecimal](jsonBytes)(bigDecimalCodec)
/* FIXME: Play-JSON: don't know how to tune precision for parsing of BigDecimal values
  @Benchmark
  def readPlayJson(): BigDecimal = Json.parse(jsonBytes).as[BigDecimal]
*/
  @Benchmark
  def readUPickle(): BigDecimal = read[BigDecimal](jsonBytes)

  @Benchmark
  def writeAVSystemGenCodec(): Array[Byte] = JsonStringOutput.write(obj).getBytes(UTF_8)

  @Benchmark
  def writeCirce(): Array[Byte] = printer.pretty(obj.asJson).getBytes(UTF_8)

  @Benchmark
  def writeDslJsonScala(): Array[Byte] = dslJsonEncode[BigDecimal](obj)

  @Benchmark
  def writeJacksonScala(): Array[Byte] = jacksonMapper.writeValueAsBytes(obj)

  @Benchmark
  def writeJsoniterScala(): Array[Byte] = writeToArray(obj)(bigDecimalCodec)

  @Benchmark
  def writeJsoniterScalaPrealloc(): Int = writeToSubArray(obj, preallocatedBuf, 0, preallocatedBuf.length)(bigDecimalCodec)
/* FIXME: Play-JSON serializes BigInt in a scientific representation (as BigDecimal)
  @Benchmark
  def writePlayJson(): Array[Byte] = Json.toBytes(Json.toJson(obj))
*/
  @Benchmark
  def writeUPickle(): Array[Byte] = write(obj).getBytes(UTF_8)
}