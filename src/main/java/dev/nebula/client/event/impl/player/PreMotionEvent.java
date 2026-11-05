package dev.nebula.client.event.impl.player;

import dev.nebula.client.event.Event;

public class PreMotionEvent extends Event {
    private static boolean setRenderYaw;
    public double posY;
    private double posX;
    private double posZ;
    private float yaw;
    private float pitch;
    private boolean onGround;
    private boolean isSprinting;
    private boolean isSneaking;

    public PreMotionEvent(double posX, double posY, double posZ, float yaw, float pitch, boolean onGround, boolean isSprinting, boolean isSneaking) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.isSprinting = isSprinting;
        this.isSneaking = isSneaking;
    }

    public static boolean setRenderYaw() {
        return setRenderYaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
        setRenderYaw = true;
    }

    public void setRenderYaw(boolean setRenderYaw) {
        PreMotionEvent.setRenderYaw = setRenderYaw;
    }

    public boolean isSprinting() {
        return isSprinting;
    }

    public void setSprinting(boolean sprinting) {
        this.isSprinting = sprinting;
    }

    public boolean isSneaking() {
        return isSneaking;
    }

    public void setSneaking(boolean sneaking) {
        this.isSneaking = sneaking;
    }

    public boolean isEquals(PlayerState e) {
        return e.x == this.posX && e.y == this.posY && e.z == this.posZ && e.yaw == this.yaw && e.pitch == this.pitch && e.onGround == this.onGround && e.isSprinting == this.isSprinting && e.isSneaking == this.isSneaking;
    }

	public double getPosY() {
		return posY;
	}

	public void setPosY(double posY) {
		this.posY = posY;
	}

	public double getPosX() {
		return posX;
	}

	public void setPosX(double posX) {
		this.posX = posX;
	}

	public double getPosZ() {
		return posZ;
	}

	public void setPosZ(double posZ) {
		this.posZ = posZ;
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = pitch;
	}

	public boolean isOnGround() {
		return onGround;
	}

	public void setOnGround(boolean onGround) {
		this.onGround = onGround;
	}

	public float getYaw() {
		return yaw;
	}
}
