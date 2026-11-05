package dev.nebula.client.event;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventManager {
    private static final EventManager INSTANCE = new EventManager();
    private final Map<Class<? extends Event>, List<ListenerMethod>> listeners = new ConcurrentHashMap<>();

    private static class ListenerMethod implements Comparable<ListenerMethod> {
        Method method;
        Object instance;
        EventPriority priority;

        ListenerMethod(Method method, Object instance, EventPriority priority) {
            this.method = method;
            this.instance = instance;
            this.priority = priority;
            this.method.setAccessible(true);
        }

        @Override
        public int compareTo(ListenerMethod other) {
            return Integer.compare(other.priority.value, this.priority.value);
        }

        void invoke(Event event) throws Exception {
            method.invoke(instance, event);
        }
    }

    public static EventManager getInstance() {
        return INSTANCE;
    }

    public synchronized void register(Object listener) {
        unregister(listener);
        Class<?> clazz = listener.getClass();
        
        for (Method method : clazz.getDeclaredMethods()) {
            EventTarget annotation = method.getAnnotation(EventTarget.class);
            
            if (annotation != null && method.getParameterCount() == 1) {
                Class<?> eventClass = method.getParameterTypes()[0];
                
                if (Event.class.isAssignableFrom(eventClass)) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Event> eventType = (Class<? extends Event>) eventClass;
                    
                    ListenerMethod listener_method = new ListenerMethod(
                        method, 
                        listener, 
                        annotation.priority()
                    );
                    
                    listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                             .add(listener_method);
                    
                    Collections.sort(listeners.get(eventType));
                }
            }
        }
    }

    public synchronized void unregister(Object listener) {
        for (List<ListenerMethod> list : listeners.values()) {
            list.removeIf(lm -> lm.instance.equals(listener));
        }
    }

    public void call(Event event) {
        List<ListenerMethod> eventListeners = listeners.get(event.getClass());
        
        if (eventListeners != null) {
            for (ListenerMethod listener : eventListeners) {
                try {
                    listener.invoke(event);
                    if (event.isCancelled()) {
                        break;
                    }
                } catch (Exception e) {
                    dev.nebula.client.utils.Log.error("Event listener failed: " + listener.method + " -> " + e.getCause());
                }
            }
        }
    }
}
