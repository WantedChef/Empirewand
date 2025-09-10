package nl.wantedchef.empirewand.core.util;

import org.jetbrains.annotations.NotNull;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Performance monitoring utility for tracking operations.
 */
public class PerformanceMonitor {
    private final Map<String, Long> operationTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> operationCounts = new ConcurrentHashMap<>();
    
    public void startOperation(@NotNull String operation) {
        operationTimes.put(operation, System.nanoTime());
    }
    
    public void endOperation(@NotNull String operation) {
        Long startTime = operationTimes.remove(operation);
        if (startTime != null) {
            operationCounts.merge(operation, 1, Integer::sum);
        }
    }
    
    public int getOperationCount(@NotNull String operation) {
        return operationCounts.getOrDefault(operation, 0);
    }
    
    public void reset() {
        operationTimes.clear();
        operationCounts.clear();
    }
}
