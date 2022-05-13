package com.dongholab.kafka.config.gson

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.*

class DateSerializer: JsonSerializer<Date> {

    val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    override fun serialize(
        src: Date,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(dateFormat.format(src))
    }

} 