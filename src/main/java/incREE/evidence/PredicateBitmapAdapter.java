package incREE.evidence;

import com.google.gson.*;
import java.lang.reflect.Type;

public class PredicateBitmapAdapter implements JsonSerializer<PredicateBitmap>, JsonDeserializer<PredicateBitmap> {
    @Override
    public JsonElement serialize(PredicateBitmap src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray jsonArray = new JsonArray();
        for (int i = src.nextSetBit(0); i >= 0; i = src.nextSetBit(i + 1)) {
            jsonArray.add(i);
        }
        return jsonArray;
    }

    @Override
    public PredicateBitmap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        // 自定义反序列化逻辑
        JsonArray jsonArray = json.getAsJsonArray();
        PredicateBitmap obj = new PredicateBitmap();
        for (JsonElement element : jsonArray) {
            obj.set(element.getAsInt());
        }
        return obj;
    }
}
