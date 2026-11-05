package dev.nebula.client.input;
public interface KeyCapture {
    boolean isListening();
    void stopListening();
    String getName();
}
