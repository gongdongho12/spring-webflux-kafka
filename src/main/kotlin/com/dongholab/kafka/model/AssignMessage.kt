package com.dongholab.kafka.model

import com.dongholab.kafka.constants.KafkaConstants

data class AssignMessage(
    val topic: String = KafkaConstants.transactionTopics.first(),
    val key: String,
    val data: Any
)
