package com.algaworks.algasensors.temperature.processsing.api.controller;

import com.algaworks.algasensors.temperature.processsing.api.model.TemperatureLogOutput;
import com.algaworks.algasensors.temperature.processsing.common.IdGenerator;
import io.hypersistence.tsid.TSID;
import lombok.RequiredArgsConstructor;
import lombok.extern.flogger.Flogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;

import static com.algaworks.algasensors.temperature.processsing.infrastructure.rabbitmq.RabbitMQConfig.TEMPERATURE_PROCESSSING_EXCHANGE;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/sensors/{sensorId}/temperatures/data")
public class TemperatureProcessingController {

    private final RabbitTemplate rabbitTemplate;

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public void data(@PathVariable TSID sensorId, @RequestBody String input) {
        if (input == null || input.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        Double temperature;

        try {
            temperature = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

        TemperatureLogOutput logOutput =TemperatureLogOutput.builder()
                .id(IdGenerator.generateTimeBasedUUID())
                .sensorId(sensorId)
                .registeredAt(OffsetDateTime.now())
                .value(temperature)
                .build();
        log.info("Received temperature: {}", logOutput.toString());

        log.info("Sending temperature: {}", logOutput);
        String routingKey = "";

        rabbitTemplate.convertAndSend(TEMPERATURE_PROCESSSING_EXCHANGE, routingKey, logOutput,
                message -> {
                    message.getMessageProperties().setHeader("sensorId", sensorId.toString());
                    return message;
                });
    }
}
