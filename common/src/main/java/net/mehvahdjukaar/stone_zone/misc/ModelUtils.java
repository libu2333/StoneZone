package net.mehvahdjukaar.stone_zone.misc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.mehvahdjukaar.every_compat.api.SimpleModule;
import net.mehvahdjukaar.stone_zone.StoneZone;
import net.mehvahdjukaar.stone_zone.api.SZModule;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//!! Tools to create a custom model for StoneZone's blocks to have tinted color
public final class ModelUtils {

    private static String RegEx = "\"parent\"\\s*:\\s*\"((.*?):(block.*?)(\\/\\w+))\"";
    private static final Pattern PARENT_PATTERN = Pattern.compile(RegEx);

    /// Modified model files won't be put in parentsToReplace
    public static final Map<ResourceLocation, ResourceLocation> modifiedParent = new HashMap<>();

    public static String replaceParent(String input, SimpleModule module) {
        Matcher matcher = PARENT_PATTERN.matcher(input);
        String RegExALT = RegEx; // prevent the incrementing
        RegExALT = ".*" + RegExALT + ",.*";

        // RegExALT - skip the files from models/item
        if (matcher.find() && Pattern.compile(RegExALT).matcher(input).find()) {

            // oldRes: minecraft:block/aa -> newRes: stonezone:block/minecraft/aa
            return matcher.replaceAll(m -> {
                ResourceLocation oldRes = new ResourceLocation(matcher.group(1));
                ResourceLocation newRes = StoneZone.res(m.group(3) + "/" + m.group(2) + m.group(4));
                if (module instanceof SZModule szModule && !modifiedParent.containsKey(oldRes) && !oldRes.getPath().contains("snow")) {
                    szModule.addParentModelToMap(oldRes, newRes);
                }
                return "\"parent\": \"" + newRes + "\"";
            });
        }
        return input;
    }

    public static String replaceCubePath(String input, ResourceLocation newRes) {
        // Check for either "minecraft:block/cube" or "block/cube"
        String RegEx = "\"parent\"\\s*:\\s*\"(minecraft)?:?(block/cube)\"";
        Matcher matcher = Pattern.compile(RegEx).matcher(input);

        if (matcher.find()) {
            return matcher.replaceFirst(m -> "\"parent\": \"" + newRes + "\"");
        }

        return input;
    }

    // parsing and then unparsing. will be sub optimal
    public static String forceSetTintIndex(String jsonText) {
        JsonObject jsonObject = GsonHelper.parse(jsonText);
        addTintIndexToModels(jsonObject, 0);
        return jsonObject.toString();
    }

    public static void addTintIndexToModels(JsonObject jsonObject, int tintIndex) {
        for (String mainKey : jsonObject.keySet()) {
            JsonElement value = jsonObject.get(mainKey);

            if (mainKey.equals("elements") && value.getAsJsonArray().isJsonArray()) {
                // Some model files (walls or stairs) have more than one array under Elements
                for (int idx = 0; idx < value.getAsJsonArray().size(); idx++) {
                    if (value.getAsJsonArray().get(idx) instanceof JsonObject underElements) {

                        // Process child objects under "elements"
                        for (String childKey : underElements.keySet()) {
                            if (childKey.equals("faces")) {
                                for (String subKeys : underElements.getAsJsonObject(childKey).keySet()) {
                                    JsonObject childObject = underElements.getAsJsonObject(childKey).getAsJsonObject(subKeys);

                                    // Add "tintindex": 0 if not present
                                    if (childObject != null && !childObject.has("tintindex")) {
                                        childObject.addProperty("tintindex", tintIndex);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else if (value instanceof JsonObject jo) {
                // Recursively process nested objects
                addTintIndexToModels(jo, tintIndex);
            }
        }
    }


}
