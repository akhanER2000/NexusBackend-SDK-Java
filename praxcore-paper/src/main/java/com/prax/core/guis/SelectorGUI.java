// Ubicación: praxcore-paper/src/main/java/com/prax/core/guis/SelectorGUI.java

package com.prax.core.guis; // <-- PAQUETE CORREGIDO

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.prax.core.PraxCorePlugin;
import com.prax.core.PluginMessageListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class SelectorGUI {

    private final PraxCorePlugin plugin;

    public SelectorGUI(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        requestPlayerCount(player, "minigame_dbd");
        requestPlayerCount(player, "builds");

        Inventory selectorInventory = Bukkit.createInventory(null, 9, "§8Selector de Servidores");

        int minigamesPlayers = PluginMessageListener.serverPlayerCounts.getOrDefault("minigame_dbd", 0);
        int buildsPlayers = PluginMessageListener.serverPlayerCounts.getOrDefault("builds", 0);

        ItemStack minigamesItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta minigamesMeta = minigamesItem.getItemMeta();
        minigamesMeta.setDisplayName("§bMinijuegos");
        minigamesMeta.setLore(Arrays.asList(
                "§7¡Haz clic para unirte a la acción!",
                "",
                "§eJugadores: §a" + minigamesPlayers
        ));
        minigamesMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        minigamesItem.setItemMeta(minigamesMeta);

        ItemStack buildsItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta buildsMeta = buildsItem.getItemMeta();
        buildsMeta.setDisplayName("§eBuilds");
        buildsMeta.setLore(Arrays.asList(
                "§7¡Un mundo para dar rienda suelta a tu creatividad!",
                "",
                "§eJugadores: §a" + buildsPlayers
        ));
        buildsItem.setItemMeta(buildsMeta);

        selectorInventory.setItem(2, minigamesItem);
        selectorInventory.setItem(6, buildsItem);

        player.openInventory(selectorInventory);
    }

    private void requestPlayerCount(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("PlayerCount");
        out.writeUTF(serverName);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}