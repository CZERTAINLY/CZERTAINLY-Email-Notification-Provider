package com.czertainly.np.email.util;

import com.czertainly.api.model.connector.notification.NotificationProviderNotifyRequestDto;
import com.czertainly.np.email.exception.NotificationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

public class TemplateUtils {

    private static final Logger logger = LoggerFactory.getLogger(TemplateUtils.class);

    public static String processFreeMarkerHtml(String html, NotificationProviderNotifyRequestDto request) {
        // Convert request to a Map instead of using the JSON node directly
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> dataModel;
        try {
            dataModel = objectMapper.convertValue(request, Map.class);
        } catch (IllegalArgumentException e) {
            logger.error("Error while converting request to Map: {}, {}", request, e.getMessage());
            throw new NotificationException("Error while converting request to Map: " + request + ", " + e.getMessage());
        }

        // Convert request to JSON
//        ObjectMapper objectMapper = new ObjectMapper();
//        JsonNode jsonNode;
//        try {
//            jsonNode = objectMapper.valueToTree(request);
//        } catch (IllegalArgumentException e) {
//            logger.error("Error while converting request to JSON: {}, {}", request, e.getMessage());
//            throw new NotificationException("Error while converting request to JSON: " + request + ", " + e.getMessage());
//        }

        // Prepare FreeMarker configuration
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);

        // Create template from the HTML string
        Template template = null;
        try {
            template = new Template("contentTemplate", new StringReader(html), cfg);
        } catch (IOException e) {
            logger.error("Error while creating FreeMarker template: {}, {}", html, e.getMessage());
            throw new NotificationException("Error while creating FreeMarker template: " + e.getMessage(), e);
        }

        // Process the template with the data model
        StringWriter stringWriter = new StringWriter();
        try {
            template.process(dataModel, stringWriter);
        } catch (TemplateException | IOException e) {
            logger.error("Error while processing FreeMarker template: {}, {}", request, e.getMessage());
            throw new NotificationException("Error while processing FreeMarker template: " + e.getMessage());
        }

        return stringWriter.toString();
    }

}
