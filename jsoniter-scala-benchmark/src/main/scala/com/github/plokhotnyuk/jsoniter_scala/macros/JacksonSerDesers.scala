package com.github.plokhotnyuk.jsoniter_scala.macros

import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.{JsonFactory, JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.afterburner.AfterburnerModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.github.plokhotnyuk.jsoniter_scala.macros.SuitEnum.SuitEnum

import scala.collection.immutable.BitSet
import scala.collection.mutable
import scala.util.Try

object JacksonSerDesers {
  val jacksonMapper: ObjectMapper with ScalaObjectMapper = new ObjectMapper(new JsonFactory {
    disable(JsonFactory.Feature.INTERN_FIELD_NAMES)
  }) with ScalaObjectMapper {
    registerModule(DefaultScalaModule)
    registerModule(new JavaTimeModule)
    registerModule(new SimpleModule()
      .addSerializer(classOf[BitSet], new BitSetSerializer)
      .addSerializer(classOf[mutable.BitSet], new MutableBitSetSerializer)
      .addSerializer(classOf[Array[Byte]], new ByteArraySerializer)
      .addSerializer(classOf[SuitADT], new SuitADTSerializer)
      .addSerializer(classOf[SuitEnum], new SuitEnumSerializer)
      .addSerializer(classOf[ZonedDateTime], new ZonedDateTimeSerializer)
      .addDeserializer(classOf[SuitADT], new SuitADTDeserializer)
      .addDeserializer(classOf[ZonedDateTime], new ZonedDateTimeDeserializer)
      .addDeserializer(classOf[SuitEnum], new SuitEnumDeserializer))
    registerModule(new AfterburnerModule)
    configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_CHAR_ARRAYS_AS_JSON_ARRAYS, true)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    configure(SerializationFeature.WRITE_DATES_WITH_ZONE_ID, true)
    configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
    setSerializationInclusion(Include.NON_EMPTY)
  }
}

class BitSetSerializer extends StdSerializer[BitSet](classOf[BitSet]) {
  override def serialize(value: BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: BitSet): Boolean = value.isEmpty
}

class MutableBitSetSerializer extends StdSerializer[mutable.BitSet](classOf[mutable.BitSet]) {
  override def serialize(value: mutable.BitSet, gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) value.foreach(gen.writeNumber)
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: mutable.BitSet): Boolean = value.isEmpty
}

class ByteArraySerializer extends StdSerializer[Array[Byte]](classOf[Array[Byte]]) {
  override def serialize(value: Array[Byte], gen: JsonGenerator, provider: SerializerProvider): Unit = {
    gen.writeStartArray()
    if (!isEmpty(provider, value)) {
      val l = value.length
      var i = 0
      while (i < l) {
        gen.writeNumber(value(i))
        i += 1
      }
    }
    gen.writeEndArray()
  }

  override def isEmpty(provider: SerializerProvider, value: Array[Byte]): Boolean = value.isEmpty
}

class ZonedDateTimeSerializer extends JsonSerializer[ZonedDateTime] {
  override def serialize(value: ZonedDateTime, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class ZonedDateTimeDeserializer extends JsonDeserializer[ZonedDateTime] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): ZonedDateTime =
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING =>
        try {
        ZonedDateTime.parse(jp.getValueAsString)
        } catch {
          case ex: DateTimeParseException =>
            ctxt.handleWeirdStringValue(classOf[ZonedDateTime], jp.getValueAsString, ex.getMessage).asInstanceOf[ZonedDateTime]
        }
      case _ =>
        ctxt.handleUnexpectedToken(classOf[ZonedDateTime], jp).asInstanceOf[ZonedDateTime]
    }
}

class SuitEnumSerializer extends JsonSerializer[SuitEnum] {
  override def serialize(value: SuitEnum, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitEnumDeserializer extends JsonDeserializer[SuitEnum] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitEnum =
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => SuitEnum.withName(jp.getValueAsString)
      case _ => ctxt.handleUnexpectedToken(classOf[SuitEnum], jp).asInstanceOf[SuitEnum]
    }
}

class SuitADTSerializer extends JsonSerializer[SuitADT] {
  override def serialize(value: SuitADT, jgen: JsonGenerator, provider: SerializerProvider): Unit =
    jgen.writeString(value.toString)
}

class SuitADTDeserializer extends JsonDeserializer[SuitADT] {
  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): SuitADT =
    jp.getCurrentToken match {
      case JsonToken.VALUE_STRING => jp.getValueAsString match {
        case "Hearts" => Hearts
        case "Spades" => Spades
        case "Diamonds" => Diamonds
        case "Clubs" => Clubs
        case s => ctxt.handleWeirdStringValue(classOf[SuitADT], s, "illegal value").asInstanceOf[SuitADT]
      }
      case _ => ctxt.handleUnexpectedToken(classOf[SuitADT], jp).asInstanceOf[SuitADT]
    }
}