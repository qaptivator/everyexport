package org.qaptivator.everyexport;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.registry.Registries;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.Recipe;
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
import java.util.HashMap;
import java.util.Map;
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
        Map<String, Object> items = new HashMap<>();
        Registries.ITEM.forEach(item -> {
            Identifier id = Registries.ITEM.getId(item);
            items.put(id.toString(), item.toString());
        });

        writeToFile(items, "items.json");
    }

    private void exportRecipes(String folder, ServerCommandSource source) throws Exception {
        var registryManager = source.getServer().getRegistryManager();
        RecipeManager recipeManager = source.getServer().getRecipeManager();
        Map<String, Object> recipes = new HashMap<>();

        for (Recipe<?> recipe : recipeManager.values()) {
            Identifier id = recipe.getId();
            recipes.put(id.toString(), recipe.getOutput(registryManager).toString());
        }

        writeToFile(recipes, "recipes.json");
    }
}
