package com.dongholab.kafka.model

import com.dongholab.kafka.model.ApiMessage

data class DataMessage(
    val apiMessage: ApiMessage,
    val data: String
)
