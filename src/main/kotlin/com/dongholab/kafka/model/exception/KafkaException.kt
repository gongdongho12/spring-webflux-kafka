package com.dongholab.kafka.model.exception

class KafkaException(msg: String?) : RuntimeException(msg) {
    companion object {
        val SEND_ERROR = KafkaException("send failed")
    }
}