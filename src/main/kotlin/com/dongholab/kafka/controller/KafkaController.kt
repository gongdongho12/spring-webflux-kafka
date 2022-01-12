package com.dongholab.kafka.controller

import com.dongholab.kafka.model.Message;
import com.dongholab.kafka.service.MessageService
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/kafka")
class KafkaController(val messageService: MessageService) {
    @PostMapping
    fun produceMessage(@RequestBody message: Mono<Message>): Mono<String> {
        return message
            .flatMap { msg: Message ->
                messageService.send(
                    msg.name,
                    msg
                )
            }
    }

    @GetMapping
    fun consumeMessage(): Flux<ServerSentEvent<Any>> {
        return messageService.receive()
    }
}