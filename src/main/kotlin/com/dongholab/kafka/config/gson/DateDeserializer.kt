package com.dongholab.kafka.config.gson

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class DateDeserializer : JsonDeserializer<Date> {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun deserialize(
        json: JsonElement, typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Date {
        return dateFormat.parse(json.asString)
    }

} 