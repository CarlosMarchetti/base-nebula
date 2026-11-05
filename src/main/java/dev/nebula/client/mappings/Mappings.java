package dev.nebula.client.mappings;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class Mappings {
    private Mappings() {}

    public static Class<?> ENTITY_CLASS;
    public static Class<?> ENTITY_PLAYER_CLASS;
    public static Class<?> ENTITY_LIVING_BASE_CLASS;
    public static Class<?> ENTITY_PLAYER_MP_CLASS;
    public static Class<?> PLAYER_CONTROLLER_MP_CLASS;

    public static Method PLAYER_CONTROLLER_ATTACK_ENTITY;
    public static Method ENTITY_IS_SPRINTING;
    public static Method ENTITY_GET_EYE_HEIGHT;
    public static Method ENTITY_PLAYER_SP_SET_SPRINTING;
    public static Method NET_HANDLER_ADD_TO_SEND_QUEUE;
    public static Method ENTITY_PLAYER_SWING_ITEM;
    public static Method ENTITY_GET_HEALTH;
    public static Method ENTITY_IS_INVISIBLE;
    public static Method ENTITY_GET_POSITION;
    public static Method ENTITY_CAN_BE_COLLIDED_WITH;
    public static Method ENTITY_PLAYER_GET_NAME;
    public static Method MINECRAFT_NET_HANDLER;
    public static Method ENTITY_GET_BOUNDING_BOX;
    public static Method ITEM_STACK_GET_ITEM;
    public static Method ITEM_GET_ID_FROM_ITEM;
    public static Method ENTITY_IS_SNEAKING;
    public static Method ENTITY_GET_ATTRIBUTE_MAP;
    public static Method ATTRIBUTE_MAP_GET_ATTRIBUTE_INSTANCE;
    public static Method ATTRIBUTE_INSTANCE_GET_BASE_VALUE;
    public static Method ATTRIBUTE_INSTANCE_SET_BASE_VALUE;
    public static Method ENTITY_IS_IN_WATER;
    public static Method ENTITY_GET_UNIQUE_ID;
    public static Method ENTITY_PLAYER_GET_CURRENT_ITEM;
    public static Method VEC3_NORMALIZE;
    public static Method VEC3_ADD_VECTOR;
    public static Method ENTITY_PLAYER_GET_DISPLAY_NAME;
    public static Method I_CHAT_COMPONENT_GET_FORMATTED_TEXT;
    public static Method NET_HANDLER_GET_NETWORK_MANAGER;

    public static Field MINECRAFT_GET_PLAYER_CONTROLLER;
    public static Field MINECRAFT_THE_PLAYER;
    public static Field MINECRAFT_GAME_SETTINGS;
    public static Field GAME_SETTINGS_KEY_BIND_SPRINT;
    public static Field MINECRAFT_CURRENT_SCREEN;
    public static Field MINECRAFT_THE_WORLD;
    public static Field WORLD_LOADED_ENTITY_LIST;
    public static Field ENTITY_PLAYER_INVENTORY;
    public static Field ENTITY_POS_X;
    public static Field ENTITY_POS_Y;
    public static Field ENTITY_POS_Z;
    public static Field ENTITY_ROTATION_YAW;
    public static Field ENTITY_ROTATION_PITCH;
    public static Field ENTITY_ID_FIELD;
    public static Field C02_PACKET_USE_ENTITY_ACTION_ATTACK;
    public static Field NETWORK_MANAGER_CHANNEL;
    public static Field AXIS_ALIGNED_BB_MIN_X;
    public static Field AXIS_ALIGNED_BB_MIN_Y;
    public static Field AXIS_ALIGNED_BB_MIN_Z;
    public static Field AXIS_ALIGNED_BB_MAX_X;
    public static Field AXIS_ALIGNED_BB_MAX_Y;
    public static Field AXIS_ALIGNED_BB_MAX_Z;
    public static Field MINECRAFT_DISPLAY_WIDTH;
    public static Field MINECRAFT_DISPLAY_HEIGHT;
    public static Field ENTITY_LIST_FIELD;
    public static Field ENTITY_MOTION_X;
    public static Field ENTITY_MOTION_Y;
    public static Field ENTITY_MOTION_Z;
    public static Field ENTITY_ON_GROUND;
    public static Field PING_FIELD;
    public static Field MOVEMENT_INPUT_FORWARD;
    public static Field MOVEMENT_INPUT_STRAFE;
    public static Field MOVE_FORWARD_FIELD;
    public static Field MOVE_STRAFING_FIELD;
    public static Field ISINWEB;
    public static Field ENTITY_HEIGHT_FIELD;
    public static Field ENTITY_PLAYER_MOVEMENT_INPUT;
    public static Field C03_PACKET_PLAYER_YAW;
    public static Field C03_PACKET_PLAYER_PITCH;
    public static Field C03_PACKET_PLAYER_ROTATING;
    public static Field ENTITY_LIVING_HURT_RESISTANT_TIME;
    public static Field ENTITY_IS_DEAD;
    public static Field VEC3_X;
    public static Field VEC3_Y;
    public static Field VEC3_Z;
    public static Field MINECRAFT_INSTANCE;
    public static Field KEY_BINDING_PRESSED;

    public static Constructor<?> C02_PACKET_USE_ENTITY_CONSTRUCTOR_ENTITY_ACTION;
    public static Constructor<?> C0E_PACKET_CLICK_WINDOW_CONSTRUCTOR;
    public static Constructor<?> C03_PACKET_PLAYER_CONSTRUCTOR;
    public static Constructor<?> C04_PACKET_PLAYER_POSITION_CONSTRUCTOR;
    public static Constructor<?> C05_PACKET_PLAYER_LOOK_CONSTRUCTOR;
    public static Constructor<?> C06_PACKET_PLAYER_POS_LOOK_CONSTRUCTOR;
    public static Constructor<?> C03_PACKET_PLAYER_NOARGS_CONSTRUCTOR;
    public static Constructor<?> C0A_PACKET_ANIMATION_CONSTRUCTOR;
    public static Constructor<?> VEC3_CONSTRUCTOR;

    private static boolean initialized;

    public static synchronized void init() throws Exception {
        if (initialized) {
            return;
        }
        try {
            MappingProfileLoader.load("/mappings/mcp-1.8.9.json", Mappings.class);
            initialized = true;
        } catch (Exception e) {
            throw new Exception("Failed to initialize the 1.8.9 mapping profile", e);
        }
    }
}
