package com.dongholab.kafka.model


//@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class ApiMessage(
//    @field:JsonProperty("host")
    val host: String = "",
//    @field:JsonProperty("method")
    val method: String = "",
//    @field:JsonProperty("type")
    val type: String = ""
)
//data class ApiMessage @ConstructorProperties("host", "method", "type") constructor(
//    val host: String = "",
//    val method: String = "",
//    val type: String = "",
//)