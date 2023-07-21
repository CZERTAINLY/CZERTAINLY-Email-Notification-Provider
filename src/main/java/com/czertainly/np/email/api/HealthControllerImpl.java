package com.czertainly.np.email.api;

import com.czertainly.api.interfaces.connector.HealthController;
import com.czertainly.api.model.common.HealthDto;
import com.czertainly.np.email.service.HealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthControllerImpl implements HealthController {

    @Autowired
    public void setHealthService(HealthService healthService) {
        this.healthService = healthService;
    }

    HealthService healthService;

    @Override
    public HealthDto checkHealth() {
        return healthService.checkHealth();
    }
}
