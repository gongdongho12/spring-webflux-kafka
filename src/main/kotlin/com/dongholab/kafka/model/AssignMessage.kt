package com.dongholab.kafka.model

import com.dongholab.kafka.constants.KafkaConstants

data class AssignMessage(
    val topic: String = KafkaConstants.topics.first(),
    val key: String,
    val data: Any
)
