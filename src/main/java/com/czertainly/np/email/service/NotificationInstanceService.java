package com.czertainly.np.email.service;

import com.czertainly.api.exception.AlreadyExistException;
import com.czertainly.api.exception.NotFoundException;
import com.czertainly.api.model.connector.notification.NotificationProviderInstanceDto;
import com.czertainly.api.model.connector.notification.NotificationProviderInstanceRequestDto;
import com.czertainly.api.model.connector.notification.NotificationProviderNotifyRequestDto;

import java.util.List;
import java.util.UUID;

public interface NotificationInstanceService {

    List<NotificationProviderInstanceDto> listNotificationInstances();

    NotificationProviderInstanceDto createNotificationInstance(NotificationProviderInstanceRequestDto request) throws AlreadyExistException;

    NotificationProviderInstanceDto getNotificationInstance(UUID uuid) throws NotFoundException;

    NotificationProviderInstanceDto updateNotificationInstance(UUID uuid, NotificationProviderInstanceRequestDto request) throws NotFoundException;

    void removeNotificationInstance(UUID uuid) throws NotFoundException;

    void sendNotification(UUID uuid, NotificationProviderNotifyRequestDto request) throws NotFoundException;


}
