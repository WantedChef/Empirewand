package nl.wantedchef.empirewand.core.util.performance;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PerformanceMonitor {
    private final Map<String, Long> metrics = new ConcurrentHashMap<>();
    
    public void startTiming(String operation) {
        metrics.put(operation + "_start", System.nanoTime());
    }
    
    public void endTiming(String operation) {
        Long start = metrics.get(operation + "_start");
        if (start != null) {
            long duration = System.nanoTime() - start;
            metrics.put(operation + "_duration", duration);
        }
    }
    
    public long getDuration(String operation) {
        return metrics.getOrDefault(operation + "_duration", 0L);
    }
}
