package dev.nebula.client.utils; 

import java.util.LinkedList;
import java.util.Queue;

public class AttackCounter {
    private Queue<Long> clicks; 
    private final long WINDOW_TIME = 1000; 

    public AttackCounter() {
        this.clicks = new LinkedList<>();
    }

    public void registerClick() {
        long currentTime = System.currentTimeMillis();
        clicks.add(currentTime);
        while (!clicks.isEmpty() && clicks.peek() < currentTime - WINDOW_TIME) {
            clicks.poll();
        }
    }

    public int getCPS() {
        long currentTime = System.currentTimeMillis();
        while (!clicks.isEmpty() && clicks.peek() < currentTime - WINDOW_TIME) {
            clicks.poll();
        }
        return clicks.size();
    }
}
