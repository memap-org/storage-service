package com.memap.storage.messaging;

import com.memap.storage.client.PaymentServiceClient;
import com.memap.storage.config.RabbitConfig;
import com.memap.storage.service.RoadmapStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionEventListener {

    private final RoadmapStorageService roadmapStorageService;
    private final PaymentServiceClient paymentServiceClient;

    @RabbitListener(queues = RabbitConfig.STORAGE_SUBSCRIPTION_CREATED_QUEUE)
    public void onSubscriptionCreated(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        if (userId == null) {
            log.warn("Received subscription.created event with null userId, skipping");
            return;
        }
        long newLimit = paymentServiceClient.getMaxStoragePerRoadmap(userId);
        log.info("subscription.created event: updating storage quota for userId={} to {}bytes", userId, newLimit);
        roadmapStorageService.updateStorageQuota(newLimit, userId);
    }

    @RabbitListener(queues = RabbitConfig.STORAGE_SUBSCRIPTION_CANCELLED_QUEUE)
    public void onSubscriptionCancelled(Map<String, Object> event) {
        String userId = (String) event.get("userId");
        if (userId == null) {
            log.warn("Received subscription.cancelled event with null userId, skipping");
            return;
        }
        long freeLimit = paymentServiceClient.getMaxStoragePerRoadmap(userId);
        log.info("subscription.cancelled event: reverting storage quota for userId={} to {}bytes", userId, freeLimit);
        roadmapStorageService.updateStorageQuota(freeLimit, userId);
    }
}
