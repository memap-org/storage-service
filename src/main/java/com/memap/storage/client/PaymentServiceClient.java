package com.memap.storage.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PaymentServiceClient {

    private static final long FREE_STORAGE_BYTES = 1_073_741_824L; // 1GB fallback
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public PaymentServiceClient(
            @Value("${app.payment.base-url:http://localhost:8087/payment}") String baseUrl,
            @Value("${app.payment.internal-api-key:}") String internalApiKey) {
        this.internalApiKey = internalApiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public long getMaxStoragePerRoadmap(String ownerId) {
        try {
            PlanLimitsResponse response = restClient.get()
                    .uri("/api/v1/subscriptions/internal/plan-limits-for-user?userId={userId}", ownerId)
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .retrieve()
                    .body(PlanLimitsResponse.class);

            if (response != null && response.data() != null) {
                long limit = response.data().maxStoragePerRoadmap();
                return limit > 0 ? limit : FREE_STORAGE_BYTES;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch plan limits for ownerId={}, falling back to FREE limit: {}", ownerId, e.getMessage());
        }
        return FREE_STORAGE_BYTES;
    }

    private record PlanLimitsResponse(int code, PlanLimits data) {}

    private record PlanLimits(int maxRoadmaps, long maxStoragePerRoadmap) {}
}
