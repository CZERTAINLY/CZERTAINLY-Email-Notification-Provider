package com.czertainly.np.email.service.impl;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.exception.ValidationError;
import com.czertainly.api.exception.ValidationException;
import com.czertainly.api.model.common.attribute.v2.content.CodeBlockAttributeContent;
import com.czertainly.api.model.common.attribute.v2.content.StringAttributeContent;
import com.czertainly.api.model.connector.notification.NotificationProviderInstanceDto;
import com.czertainly.api.model.connector.notification.NotificationProviderInstanceRequestDto;
import com.czertainly.api.model.connector.notification.NotificationProviderNotifyRequestDto;
import com.czertainly.api.model.connector.notification.NotificationRecipientDto;
import com.czertainly.core.util.AttributeDefinitionUtils;
import com.czertainly.np.email.dao.entity.NotificationInstance;
import com.czertainly.np.email.dao.repository.NotificationInstanceRepository;
import com.czertainly.np.email.exception.NotificationException;
import com.czertainly.np.email.service.AttributeService;
import com.czertainly.np.email.service.NotificationInstanceService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationInstanceServiceImpl implements NotificationInstanceService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationInstanceServiceImpl.class);

    private NotificationInstanceRepository notificationInstanceRepository;

    private JavaMailSender emailSender;

    private AttributeService attributeService;

    @Autowired
    public void setNotificationInstanceRepository(NotificationInstanceRepository notificationInstanceRepository) {
        this.notificationInstanceRepository = notificationInstanceRepository;
    }

    @Autowired
    public void setEmailSender(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Autowired
    public void setAttributeService(AttributeService attributeService) {
        this.attributeService = attributeService;
    }

    @Override
    public List<NotificationProviderInstanceDto> listNotificationInstances() {
        List<NotificationInstance> instances;
        instances = notificationInstanceRepository.findAll();
        if (!instances.isEmpty()) {
            return instances
                    .stream().map(NotificationInstance::mapToDto)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    public NotificationProviderInstanceDto createNotificationInstance(NotificationProviderInstanceRequestDto request) throws AlreadyExistException {
        if (notificationInstanceRepository.findByName(request.getName()).isPresent()) {
            throw new AlreadyExistException(NotificationInstance.class, request.getName());
        }

        final String emailFrom = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_SENDER_EMAIL_ADDRESS_NAME, request.getAttributes(), StringAttributeContent.class).getData();

        final String subject = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_SUBJECT_NAME, request.getAttributes(), StringAttributeContent.class).getData();

        final String contentTemplate = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_CONTENT_TEMPLATE_NAME, request.getAttributes(), CodeBlockAttributeContent.class).getData().getCode();

        NotificationInstance notificationInstance = new NotificationInstance();
        notificationInstance.setUuid(UUID.randomUUID().toString());
        notificationInstance.setName(request.getName());
        notificationInstance.setAttributes(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes()));
        notificationInstance.setEmailFrom(emailFrom);
        notificationInstance.setSubject(subject);
        notificationInstance.setContentTemplate(contentTemplate);

        notificationInstanceRepository.save(notificationInstance);

        return notificationInstance.mapToDto();
    }

    @Override
    public NotificationProviderInstanceDto getNotificationInstance(UUID uuid) throws NotFoundException {
        return notificationInstanceRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(NotificationInstance.class, uuid))
                .mapToDto();
    }

    @Override
    public NotificationProviderInstanceDto updateNotificationInstance(UUID uuid, NotificationProviderInstanceRequestDto request) throws NotFoundException {
        NotificationInstance notificationInstance = notificationInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(NotificationInstance.class, uuid));

        final String emailFrom = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_SENDER_EMAIL_ADDRESS_NAME, request.getAttributes(), StringAttributeContent.class).getData();

        final String subject = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_SUBJECT_NAME, request.getAttributes(), StringAttributeContent.class).getData();

        final String contentTemplate = AttributeDefinitionUtils.getSingleItemAttributeContentValue(
                AttributeServiceImpl.DATA_CONTENT_TEMPLATE_NAME, request.getAttributes(), CodeBlockAttributeContent.class).getData().getCode();

        notificationInstance.setAttributes(AttributeDefinitionUtils.mergeAttributes(attributeService.getAttributes(request.getKind()), request.getAttributes()));
        notificationInstance.setEmailFrom(emailFrom);
        notificationInstance.setSubject(subject);
        notificationInstance.setContentTemplate(contentTemplate);

        notificationInstanceRepository.save(notificationInstance);

        return notificationInstance.mapToDto();
    }

    @Override
    public void removeNotificationInstance(UUID uuid) throws NotFoundException {
        NotificationInstance instance = notificationInstanceRepository.findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(NotificationInstance.class, uuid));

        notificationInstanceRepository.delete(instance);
    }

    @Override
    public void sendNotification(UUID uuid, NotificationProviderNotifyRequestDto request) throws NotFoundException {
        logger.info("Received request to send email: eventType={}, resource={}", request.getEventType(), request.getResource());
        NotificationInstance notificationInstance = notificationInstanceRepository
                .findByUuid(uuid)
                .orElseThrow(() -> new NotFoundException(NotificationInstance.class, uuid));

        logger.debug("Request to send email received with the content: {}", request);

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

        String htmlMsg = notificationInstance.getContentTemplate();
        String Subject = notificationInstance.getSubject();

        // convert request to JSON for JSONPath and substitution
        ObjectWriter ow = new ObjectMapper().writer();
        String json;
        try {
            json = ow.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.error("Error while serializing request to JSON: {}, {}", request, e.getMessage());
            throw new NotificationException("Error while serializing request to JSON: " + request + ", " + e.getMessage());
        }

        Object document = Configuration.defaultConfiguration().jsonProvider().parse(json);

        StrSubstitutor subst = new StrSubstitutor(new StrLookup() {
            @Override
            public String lookup(String key) {
                try {
                    return JsonPath.read(document, "$." + key);
                } catch (Exception e) {
                    logger.error("Error while substituting key: {}, {}", key, e.getMessage());
                }
                return "";
            }
        });

        final String substitutedHtmlMsg = subst.replace(htmlMsg);
        final String substitutedSubject = subst.replace(Subject);

        try {
            helper.setText(substitutedHtmlMsg, true);
            helper.setTo(getRecipients(request.getRecipients()));
            helper.setSubject(substitutedSubject);
            helper.setFrom(notificationInstance.getEmailFrom());
        } catch (MessagingException e) {
            logger.error("Error while sending email: {}", e.getMessage());
            throw new NotificationException("Error while sending email: " + e.getMessage());
        }

        emailSender.send(mimeMessage);
        logger.info("Email sent to: {}", request.getRecipients());
    }

    private String[] getRecipients(List<NotificationRecipientDto> recipients) {
        List<String> to = new ArrayList<>();
        for (NotificationRecipientDto recipient : recipients) {
            if (StringUtils.isBlank(recipient.getEmail())) {
                logger.debug("No email address is provided for recipient: {}", recipient);
                throw new ValidationException(List.of(new ValidationError("email", "Email is required")));
            } else {
                to.add(recipient.getEmail());
            }
        }
        return to.toArray(new String[0]);
    }
}
