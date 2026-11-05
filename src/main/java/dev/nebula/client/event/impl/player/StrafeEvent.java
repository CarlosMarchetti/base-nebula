package dev.nebula.client.event.impl.player;

import dev.nebula.client.event.Event;

public class StrafeEvent extends Event {
    private float strafe;
    private float forward;
    private float friction;

    public StrafeEvent(float strafe, float forward, float friction) {
        this.strafe = strafe;
        this.forward = forward;
        this.friction = friction;
    }

    public float getStrafe() { return strafe; }
    public float getForward() { return forward; }
    public float getFriction() { return friction; }

    public void setStrafe(float strafe) { this.strafe = strafe; }
    public void setForward(float forward) { this.forward = forward; }
    public void setFriction(float friction) { this.friction = friction; }
}
