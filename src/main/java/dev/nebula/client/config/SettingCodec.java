package dev.nebula.client.config;
import dev.nebula.client.gui.settings.Setting;
import dev.nebula.client.gui.settings.impl.*;
import com.google.gson.*;
import java.math.BigDecimal;
import java.util.*;
public final class SettingCodec {
    private static final Gson JSON=new Gson();
    private SettingCodec(){}
    public static String encode(Setting setting){
        Object value=setting.getValue();
        return setting instanceof ListSetting || setting instanceof RangeSetting ? JSON.toJson(value) : String.valueOf(value);
    }
    public static Object decode(Setting setting,String raw){
        if(setting instanceof BooleanSetting){if(!raw.equals("true")&&!raw.equals("false"))throw new IllegalArgumentException("Invalid boolean");return Boolean.valueOf(raw);}
        if(setting instanceof IntSetting || setting instanceof KeyBindSetting)return new BigDecimal(raw).intValueExact();
        if(setting instanceof DoubleSetting){double n=Double.parseDouble(raw);if(!Double.isFinite(n))throw new IllegalArgumentException("Nonfinite number");return n;}
        if(setting instanceof FloatSetting){float n=Float.parseFloat(raw);if(!Float.isFinite(n))throw new IllegalArgumentException("Nonfinite number");return n;}
        if(setting instanceof ColorPickerSetting){long n=Long.decode(raw);if(n<Integer.MIN_VALUE||n>0xffffffffL)throw new IllegalArgumentException("Invalid ARGB");return (int)n;}
        if(setting instanceof ComboBoxSetting){for(String choice:((ComboBoxSetting)setting).getOptions())if(choice.equals(raw))return raw;throw new IllegalArgumentException("Unknown choice");}
        JsonElement element=new JsonParser().parse(raw);
        if(!element.isJsonArray())throw new IllegalArgumentException("Expected array");
        JsonArray array=element.getAsJsonArray();
        if(setting instanceof ListSetting){List<String> result=new ArrayList<>();for(JsonElement e:array){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected string list");result.add(e.getAsString());}return result;}
        if(setting instanceof RangeSetting){if(array.size()!=2)throw new IllegalArgumentException("Expected two bounds");double a=array.get(0).getAsDouble(),b=array.get(1).getAsDouble();if(!Double.isFinite(a)||!Double.isFinite(b)||a>b)throw new IllegalArgumentException("Invalid interval");return new double[]{a,b};}
        throw new IllegalArgumentException("Unsupported setting type: "+setting.getType());
    }
    public static String encodeStrings(java.util.List<String> values){return JSON.toJson(values);}
    public static java.util.List<String> decodeStrings(String raw){
        java.util.List<String> values=new java.util.ArrayList<>();
        if(!raw.trim().startsWith("[")){for(String value:raw.split(","))if(!value.isEmpty())values.add(value);return values;}
        JsonArray array=new JsonParser().parse(raw).getAsJsonArray();
        for(JsonElement item:array){if(!item.isJsonPrimitive()||!item.getAsJsonPrimitive().isString())throw new IllegalArgumentException("Expected string list");values.add(item.getAsString());}
        return values;
    }
    public static String[] describe(Setting setting){
        String type,min="0",max="100";String[] options=new String[0];
        if(setting instanceof IntSetting){type="integer";min=""+((IntSetting)setting).getMin();max=""+((IntSetting)setting).getMax();}
        else if(setting instanceof DoubleSetting){type="double";min=""+((DoubleSetting)setting).getMin();max=""+((DoubleSetting)setting).getMax();}
        else if(setting instanceof FloatSetting){type="double";min=""+((FloatSetting)setting).getMin();max=""+((FloatSetting)setting).getMax();}
        else if(setting instanceof BooleanSetting)type="boolean";
        else if(setting instanceof ComboBoxSetting){type="combo";options=((ComboBoxSetting)setting).getOptions();}
        else if(setting instanceof ColorPickerSetting)type="color";
        else if(setting instanceof KeyBindSetting)type="keybind";
        else if(setting instanceof ListSetting){type="list";options=((java.util.List<?>)setting.getValue()).toArray(new String[0]);}
        else if(setting instanceof RangeSetting){RangeSetting range=(RangeSetting)setting;type="range";min=""+range.getMin();max=""+range.getMax();}
        else throw new IllegalArgumentException("Unsupported setting: "+setting.getType());
        String[] row=new String[5+options.length];row[0]=setting.getName();row[1]=type;row[2]=encode(setting);row[3]=min;row[4]=max;
        System.arraycopy(options,0,row,5,options.length);return row;
    }
    public static void apply(Setting setting,String raw){setting.setValue(decode(setting,raw));}
}
