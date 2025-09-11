package nl.wantedchef.empirewand.framework.service.metrics;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Internal debug metrics service for tracking performance statistics.
 * Uses a ring buffer to maintain recent performance data for P95 calculations.
 */
public class DebugMetricsService {

    private final ConcurrentLinkedQueue<Long> spellCastTimes = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Long> eventProcessingTimes = new ConcurrentLinkedQueue<>();
    private final AtomicLong totalSpellCasts = new AtomicLong(0);
    private final AtomicLong totalFailedCasts = new AtomicLong(0);
    private final AtomicLong totalWandsCreated = new AtomicLong(0);
    private final Set<String> activeWands = ConcurrentHashMap.newKeySet();
    private final int maxSamples;

    public DebugMetricsService(int maxSamples) {
        this.maxSamples = maxSamples;
    }

    /**
     * Records a spell cast timing.
     */
    public void recordSpellCast(long durationMs) {
        totalSpellCasts.incrementAndGet();
        addSample(spellCastTimes, durationMs);
    }

    /**
     * Records a failed spell cast.
     */
    public void recordFailedCast() {
        totalFailedCasts.incrementAndGet();
    }

    /**
     * Records a wand creation.
     */
    public void recordWandCreated(String wandId) {
        totalWandsCreated.incrementAndGet();
        activeWands.add(wandId);
    }

    /**
     * Records a wand destruction/removal.
     */
    public void recordWandRemoved(String wandId) {
        activeWands.remove(wandId);
    }

    /**
     * Records event processing timing.
     */
    public void recordEventProcessing(long durationMs) {
        addSample(eventProcessingTimes, durationMs);
    }

    /**
     * Gets the P95 spell cast time.
     */
    public long getSpellCastP95() {
        return calculateP95(spellCastTimes);
    }

    /**
     * Gets the P95 event processing time.
     */
    public long getEventProcessingP95() {
        return calculateP95(eventProcessingTimes);
    }

    /**
     * Gets the total number of spell casts.
     */
    public long getTotalSpellCasts() {
        return totalSpellCasts.get();
    }

    /**
     * Gets the total number of failed casts.
     */
    public long getTotalFailedCasts() {
        return totalFailedCasts.get();
    }

    /**
     * Gets the total number of wands created.
     */
    public long getTotalWandsCreated() {
        return totalWandsCreated.get();
    }

    /**
     * Gets the number of active wands.
     */
    public int getActiveWandsCount() {
        return activeWands.size();
    }

    /**
     * Gets the spell cast success rate as a percentage.
     */
    public double getSpellCastSuccessRate() {
        long total = totalSpellCasts.get() + totalFailedCasts.get();
        if (total == 0)
            return 100.0;
        return (totalSpellCasts.get() * 100.0) / total;
    }

    /**
     * Clears all metrics data.
     */
    public void clear() {
        spellCastTimes.clear();
        eventProcessingTimes.clear();
        totalSpellCasts.set(0);
        totalFailedCasts.set(0);
        totalWandsCreated.set(0);
        activeWands.clear();
    }

    /**
     * Adds a sample to the ring buffer, maintaining max size.
     */
    private void addSample(ConcurrentLinkedQueue<Long> queue, long value) {
        queue.offer(value);
        // Maintain ring buffer size
        while (queue.size() > maxSamples) {
            queue.poll();
        }
    }

    /**
     * Calculates the P95 value from the samples.
     */
    private long calculateP95(ConcurrentLinkedQueue<Long> queue) {
        if (queue.isEmpty()) {
            return 0;
        }
        
        // Convert to array and sort
        Long[] samples = queue.toArray(new Long[0]);
        java.util.Arrays.sort(samples);
        
        // Calculate P95 index
        int index = (int) Math.ceil(0.95 * samples.length) - 1;
        if (index < 0) index = 0;
        if (index >= samples.length) index = samples.length - 1;
        
        return samples[index];
    }

    /**
     * Gets debug information as a formatted string.
     */
    public String getDebugInfo() {
        return String.format(
                "Debug Metrics:%n" +
                        "  Total Spell Casts: %d%n" +
                        "  Total Failed Casts: %d%n" +
                        "  Success Rate: %.2f%%%n" +
                        "  Spell Cast P95: %dms%n" +
                        "  Event Processing P95: %dms%n" +
                        "  Active Samples: %d spell casts, %d events%n" +
                        "  Total Wands Created: %d%n" +
                        "  Active Wands: %d",
                getTotalSpellCasts(),
                getTotalFailedCasts(),
                getSpellCastSuccessRate(),
                getSpellCastP95(),
                getEventProcessingP95(),
                spellCastTimes.size(),
                eventProcessingTimes.size(),
                getTotalWandsCreated(),
                getActiveWandsCount());
    }
}
