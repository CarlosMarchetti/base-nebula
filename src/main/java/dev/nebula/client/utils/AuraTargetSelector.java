package dev.nebula.client.utils;
import java.util.List;
public final class AuraTargetSelector {
    private Integer current;
    private long selectedAt;
    public Integer select(List<Integer> ordered, String mode, long now, long delay) {
        if (ordered.isEmpty()) { reset(); return null; }
        int index = current == null ? -1 : ordered.indexOf(current);
        if (index < 0) { current = ordered.get(0); selectedAt = now; }
        else if ("Switch".equals(mode) && now - selectedAt >= delay) {
            current = ordered.get((index + 1) % ordered.size()); selectedAt = now;
        } else if ("Multi".equals(mode)) { current = ordered.get(0); }
        return current;
    }
    public void reset() { current = null; selectedAt = 0; }
}
