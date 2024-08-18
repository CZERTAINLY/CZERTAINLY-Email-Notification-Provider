package com.czertainly.np.email.util;

import com.czertainly.api.model.common.HealthDto;
import com.czertainly.api.model.common.HealthStatus;
import com.czertainly.api.model.connector.notification.NotificationProviderNotifyRequestDto;
import com.czertainly.api.model.connector.notification.NotificationRecipientDto;
import com.czertainly.api.model.connector.notification.NotificationType;
import com.czertainly.api.model.connector.notification.data.NotificationDataCertificateStatusChanged;
import com.czertainly.api.model.core.auth.Resource;
import com.czertainly.np.email.service.HealthService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

@SpringBootTest
public class TemplateUtilsTest {

    @MockBean
    private HealthService healthService;

    @MockBean
    private JavaMailSender javaMailSender;

    private static final String CERT_STATUS_CHANGED_HTML_TEMPLATE = """
<h3>Certificate status change!</h3>

<p>
  The certificate identified as:
  <ul>
    <li>Subject: ${notificationData.subjectDn}</li>
    <li>Serial Number: ${notificationData.serialNumber}</li>
    <li>Issuer: ${notificationData.issuerDn}</li>
  </ul>
</p>

<button onclick="location.href='https://yourdomain.com/administrator/#/certificates/detail/${notificationData.certificateUuid}'"
        type="button">
  Go To Certificate
</button>
""";

    @BeforeEach
    public void setUp() {
        // Define behavior for the mock if needed
        HealthDto healthDto = new HealthDto();
        healthDto.setStatus(HealthStatus.OK);
        Mockito.when(healthService.checkHealth()).thenReturn(healthDto);
    }

    @Test
    public void testTemplateUtils() {
        NotificationDataCertificateStatusChanged notificationData = new NotificationDataCertificateStatusChanged();
        notificationData.setSubjectDn("CN=Test Certificate");
        notificationData.setSerialNumber("123456");
        notificationData.setIssuerDn("CN=Test CA");
        notificationData.setCertificateUuid("654321");
        notificationData.setFingerprint("987654321");

        NotificationRecipientDto recipient = new NotificationRecipientDto();
        recipient.setEmail("test@czertainly.com");
        recipient.setName("Test User");

        NotificationProviderNotifyRequestDto request = new NotificationProviderNotifyRequestDto();
        request.setEventType(NotificationType.CERTIFICATE_STATUS_CHANGED);
        request.setResource(Resource.CERTIFICATE);
        request.setRecipients(List.of(recipient));
        request.setNotificationData(notificationData);

        String processedHtmlTemplate = TemplateUtils.processFreeMarkerHtml(CERT_STATUS_CHANGED_HTML_TEMPLATE, request);

        Assertions.assertTrue(processedHtmlTemplate.contains("Certificate status change!"));
    }

}
