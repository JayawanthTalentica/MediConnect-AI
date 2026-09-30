package com.mediconnect.service;

import com.mediconnect.entity.Appointment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class EventPublisherService {

    private static final Logger log = LoggerFactory.getLogger(EventPublisherService.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    public EventPublisherService(KafkaTemplate<String, String> kafkaTemplate, ApplicationEventPublisher applicationEventPublisher) {
        this.kafkaTemplate = kafkaTemplate;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishAppointmentBookedEvent(Appointment appointment) {
        String eventMessage = String.format(
            "{\"eventId\":\"evt_%d\",\"eventType\":\"APPOINTMENT_BOOKED\",\"patientName\":\"%s\",\"doctorName\":\"%s\",\"slotTime\":\"%s\",\"status\":\"%s\"}",
            appointment.getId(),
            appointment.getPatient().getName(),
            appointment.getDoctor().getName(),
            appointment.getSlot().getSlotTime(),
            appointment.getStatus()
        );

        log.info("📢 PUBLISHING KAFKA EVENT -> Topic: 'appointment-events' | Message: {}", eventMessage);

        try {
            kafkaTemplate.send("appointment-events", String.valueOf(appointment.getId()), eventMessage);
        } catch (Exception e) {
            log.warn("Kafka broker unreachable. Falling back to Spring ApplicationEvent: {}", e.getMessage());
        }

        applicationEventPublisher.publishEvent(appointment);
    }
}
