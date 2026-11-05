package dev.nebula.client.gui.settings.impl;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.modules.Module;
import org.lwjgl.input.Keyboard;
public final class KeyBindSetting extends Setting {
    private int standaloneKey;
    public KeyBindSetting(String name,Module module,int initial){super(name,module,"KeyBind");standaloneKey=initial;}
    @Override public Object getValue(){return getKeyCode();}
    @Override public void setValue(Object value){if(!(value instanceof Integer))throw new IllegalArgumentException("Expected integer binding");if(parentModule==null)standaloneKey=(Integer)value;else parentModule.setKey((Integer)value);save();}
    public int getKeyCode(){return parentModule==null?standaloneKey:parentModule.getKey();}
    public void setKeyCode(int key){setValue(key);}
    public String getKeyName(){return Keyboard.getKeyName(getKeyCode());}
}
