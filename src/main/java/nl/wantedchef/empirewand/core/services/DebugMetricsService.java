package nl.wantedchef.empirewand.core.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class DebugMetricsService {
    private final int maxSamples;
    private final Map<String, Long> metrics = new ConcurrentHashMap<>();
    
    public DebugMetricsService(int maxSamples) {
        this.maxSamples = maxSamples;
    }
    
    public void record(String key, long value) {
        metrics.put(key, value);
    }
    
    public Long get(String key) {
        return metrics.get(key);
    }
    
    public Map<String, Long> getAllMetrics() {
        return new ConcurrentHashMap<>(metrics);
    }
    
    public void clear() {
        metrics.clear();
    }
}
