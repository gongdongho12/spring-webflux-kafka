package com.dongholab.kafka.constants

object KafkaConstants {
    const val host = "localhost:9092"
    val topics = setOf(
        "default",
        "retry1",
        "retry2",
        "retry3",
        "error"
    )
    const val API_KEY = "API"
    const val DATA_KEY = "DATA"
    const val groupId = "bb"
}