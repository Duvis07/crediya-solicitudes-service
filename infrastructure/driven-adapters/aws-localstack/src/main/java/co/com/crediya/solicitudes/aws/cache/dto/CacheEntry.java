package co.com.crediya.solicitudes.aws.cache.dto;


import co.com.crediya.solicitudes.model.lambda.CapacityCalculationResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CacheEntry {
    private CapacityCalculationResult result;
    private LocalDateTime timestamp;


    public boolean isExpired(Duration cacheExpiry) {
        return LocalDateTime.now().isAfter(timestamp.plus(cacheExpiry));
    }
}
