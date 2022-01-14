# spring-webflux-kafka
카프카 테스트 어플리케이션

## Tech Stack
- Kotlin
- SpringBoot
- Reactor Kafka
- WebFlux

## Docker를 통한 Zookeeper + Kafka + KafkaManager
kafka-gui.yml
```yaml
version: "2.2"
services:
  zookeeper:
    image: zookeeper
    restart: always
    container_name: zookeeper
    hostname: zookeeper
    ports:
      - 2181:2181
    environment:
      ZOO_MY_ID: 1
  kafka:
    image: wurstmeister/kafka
    container_name: kafka
    ports:
    - 9092:9092
    environment:
      KAFKA_ADVERTISED_HOST_NAME: 127.0.0.1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
  kafka_manager:
    image: hlebalbau/kafka-manager:stable
    container_name: kakfa-manager
    restart: always
    ports:
      - "9000:9000"
    environment:
      ZK_HOSTS: "zookeeper:2181"
      APPLICATION_SECRET: "random-secret"
    command: -Dpidfile.path=/dev/null
```
실행
```bash
docker-compose -f ./kafka-gui.yml up
```