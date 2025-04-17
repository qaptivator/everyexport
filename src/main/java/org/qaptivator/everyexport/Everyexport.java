package org.qaptivator.everyexport;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.*;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import  net.minecraft.text.MutableText;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
//import net.minecraft.command.CommandRegistryAccess;
//import net.minecraft.server.command.CommandManager.RegistrationEnvironment;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Paths;
import java.util.*;
import java.awt.Desktop;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import static net.minecraft.server.command.CommandManager.literal;

public class Everyexport implements ModInitializer {

    public static final String MOD_ID = "everyexport";

    private static final SuggestionProvider<ServerCommandSource> EXPORT_TYPE_SUGGESTIONS = (context, builder) -> {
        return CommandSource.suggestMatching(new String[]{"all", "items", "recipes"}, builder);
    };

    public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("everyexport")
                    //.requires(source -> source.hasPermissionLevel(2))
                    .then(CommandManager.argument("type", StringArgumentType.word())
                            .suggests(EXPORT_TYPE_SUGGESTIONS)
                                .executes(context -> {
                                        // formatted returns MutableText, styled returns Text
                                        MutableText messagePrefix = Text.literal("[EveryExport]")
                                                .formatted(Formatting.AQUA, Formatting.BOLD);

                                        String type = StringArgumentType.getString(context, "type");
                                        String folder = MOD_ID; // StringArgumentType.getString(context, "folder");
                                        String fullPath = Paths.get("config", folder).toAbsolutePath().toString();

                                        ServerCommandSource source = context.getSource();
                                        ServerPlayerEntity player = source.getPlayer();

                                        try {
                                            if (type.equals("all") || type.equals("items")) {
                                                exportItems(folder);
                                            }
                                            if (type.equals("all") || type.equals("recipes")) {
                                                exportRecipes(folder, source);
                                            }

                                            player.sendMessage(messagePrefix
                                                    .append(Text.literal(" Exported " + type + " as JSON ").styled(style -> style.withColor(Formatting.WHITE)
                                                            .withBold(false)
                                                            .withUnderline(false)
                                                    ))
                                                    .append(Text.literal("[Copy the full path]").styled(style -> style.withColor(Formatting.GOLD)
                                                            .withBold(false)
                                                            .withUnderline(true)
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, fullPath)))),
                                                    false);

                                            /*Text clickable = Text.literal("[Open folder]")
                                                    .styled(style -> style.withColor(0x00AAFF)
                                                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/everyexport_openfolder " + folder))
                                                            .withUnderline(true));*/

                                            //player.sendMessage(Text.of("[EveryExport] Exported " + type + " to folder: " + folder), false);
                                            //player.sendMessage(clickable, false);
                                        } catch (Exception e) {
                                            //player.sendMessage(Text.of("[EveryExport] Error: " + e.getMessage()), false);
                                        }

                                        return 1;
                                    })));

            // todo: add "open folder" clickable box in export command output, possibly made using "Desktop.getDesktop().open(dir);"
            // todo: add a config option to limit this command to op players only, possibly with ".requires(source -> source.hasPermissionLevel(2))"
            // todo: make a type alias for "Map<String, Object>"
        });
    }

    private void writeToFile(Object map, String name) throws Exception {
        File dir = Paths.get("config", MOD_ID).toFile();
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, name);
        FileWriter writer = new FileWriter(file);
        writer.write(gson.toJson(map));
        writer.close();
    }

    private void exportItems(String folder) throws Exception {
        Map<String, Object> itemsMap = new HashMap<>();

        Registries.ITEM.forEach(item -> {
            Identifier id = Registries.ITEM.getId(item);
            Map<String, Object> itemMap = new HashMap<>();

            // common properties
            itemMap.put("id", id.toString());
            itemMap.put("namespace", id.getNamespace());
            itemMap.put("path", id.getPath());
            itemMap.put("translation_key", item.getTranslationKey());
            itemMap.put("display_name", Text.translatable(item.getTranslationKey()).getString());
            itemMap.put("max_stack_size", item.getMaxCount());
            itemMap.put("max_durability", item.getMaxDamage());
            itemMap.put("is_damageable", item.isDamageable());
            itemMap.put("is_fireproof", item.isFireproof());

            // recipe remainder
            if (item.getRecipeRemainder() != null) {
                itemMap.put("recipe_remainder", Registries.ITEM.getId(item.getRecipeRemainder()).toString());
            }

            // food
            if (item.isFood()) {
                Map<String, Object> foodMap = new HashMap<>();
                FoodComponent food = item.getFoodComponent();

                foodMap.put("food_hunger", food.getHunger());
                foodMap.put("food_saturation", food.getSaturationModifier());
                foodMap.put("can_always_eat", food.isAlwaysEdible());
                foodMap.put("fast_to_eat", food.isSnack());

                itemMap.put("food", foodMap);
            } else {
                itemMap.put("food", false);
            }

            itemsMap.put(id.toString(), itemMap);
        });

        writeToFile(itemsMap, "items.json");
    }

    private static final char[] SYMBOLS = initSymbols();

    private static char[] initSymbols() {
        List<Character> symbols = new ArrayList<>();
        symbols.add('#');
        for (char c = 'A'; c <= 'Z'; c++) symbols.add(c);
        for (char c = 'a'; c <= 'z'; c++) symbols.add(c);
        for (char c = '0'; c <= '9'; c++) symbols.add(c);
        char[] array = new char[symbols.size()];
        for (int i = 0; i < symbols.size(); i++) {
            array[i] = symbols.get(i);
        }
        return array;
    }


    private void exportRecipes(String folder, ServerCommandSource source) throws Exception {
        DynamicRegistryManager registryManager = source.getServer().getRegistryManager();
        RecipeManager recipeManager = source.getServer().getRecipeManager();
        Map<String, Object> recipesMap = new HashMap<>();

        for (Recipe<?> recipe : recipeManager.values()) {
            Map<String, Object> recipeMap = new HashMap<>();

            // common properties
            recipeMap.put("id", recipe.getId().toString());
            recipeMap.put("type", recipe.getType().toString());

            // output
            ItemStack output = recipe.getOutput(registryManager);
            recipeMap.put("output", Map.of(
                    "item", Registries.ITEM.getId(output.getItem()).toString(),
                    "count", output.getCount()
            ));

            // ingredients
            List<Ingredient> ingredients = recipe.getIngredients();
            if (recipe instanceof ShapedRecipe shaped) {
                recipeMap.put("shaped", true);

                int width = shaped.getWidth();
                int height = shaped.getHeight();

                recipeMap.put("width", width);
                recipeMap.put("height", height);

                Map<String, Character> itemToSymbol = new HashMap<>();
                Map<Character, Map<String, Object>> keyMap = new HashMap<>();
                char nextChar = 'A';

                String[] pattern = new String[height];

                for (int row = 0; row < height; row++) {
                    StringBuilder rowPattern = new StringBuilder();
                    for (int col = 0; col < width; col++) {
                        int index = row * width + col;
                        Ingredient ingredient = ingredients.get(index);

                        if (ingredient.isEmpty()) {
                            rowPattern.append(" ");
                        } else {
                            // use the first matching item
                            ItemStack[] matches = ingredient.getMatchingStacks();
                            if (matches.length == 0) {
                                rowPattern.append("?");
                                continue;
                            }

                            String itemId = Registries.ITEM.getId(matches[0].getItem()).toString();

                            // reuse symbol if we've already assigned one
                            Character symbol = itemToSymbol.get(itemId);
                            if (symbol == null) {
                                symbol = nextChar++;
                                itemToSymbol.put(itemId, symbol);
                                keyMap.put(symbol, Map.of("item", itemId));
                            }

                            rowPattern.append(symbol);
                        }
                    }
                    pattern[row] = rowPattern.toString();
                }

                recipeMap.put("pattern", pattern);
                recipeMap.put("key", keyMap);
            } else {
                recipeMap.put("shaped", false);

                List<Map<String, Object>> simpleIngredients = new ArrayList<>();
                for (Ingredient ingredient : ingredients) {
                    if (ingredient.isEmpty()) {
                        simpleIngredients.add(Map.of("empty", true));
                        continue;
                    }

                    List<String> matchingItems = Arrays.stream(ingredient.getMatchingStacks())
                            .map(stack -> Registries.ITEM.getId(stack.getItem()).toString())
                            .distinct()
                            .toList();

                    simpleIngredients.add(Map.of("options", matchingItems));
                }
                recipeMap.put("ingredients", simpleIngredients);
            }

            // smelting/blasting/brewing/etc
            if (recipe instanceof AbstractCookingRecipe cooking) {
                Map<String, Object> cookMap = new HashMap<>();
                cookMap.put("time", cooking.getCookTime());
                cookMap.put("experience", cooking.getExperience());
                recipeMap.put("cook", cookMap);
            } else {
                recipeMap.put("cook", false);
            }

            recipesMap.put(recipe.getId().toString(), recipeMap);
        }

        writeToFile(recipesMap, "recipes.json");
    }
}
