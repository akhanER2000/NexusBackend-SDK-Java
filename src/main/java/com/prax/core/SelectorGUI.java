package com.prax.core.guis;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.prax.core.PraxCorePlugin; // <-- IMPORTACIÓN NUEVA
import com.prax.core.PluginMessageListener; // <-- IMPORTACIÓN NUEVA
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays; // <-- IMPORTACIÓN NUEVA

public class SelectorGUI {

    private final PraxCorePlugin plugin;

    // Constructor para tener acceso al plugin principal
    public SelectorGUI(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        // --- PASO 1: PEDIR LA INFORMACIÓN AL PROXY ---
        // Enviamos una solicitud "PlayerCount" para cada servidor que nos interesa.
        // El proxy nos responderá, y nuestro PluginMessageListener guardará la información.
        requestPlayerCount(player, "minigame_dbd");
        requestPlayerCount(player, "builds");

        // Creamos un inventario con 9 slots (1 fila) y un título.
        Inventory selectorInventory = Bukkit.createInventory(null, 9, "§8Selector de Servidores");

        // --- PASO 2: USAR LA INFORMACIÓN RECIBIDA ---
        // Obtenemos el conteo de jugadores desde el mapa de nuestro listener.
        // Usamos .getOrDefault() para mostrar 0 si aún no hemos recibido la información.
        int minigamesPlayers = PluginMessageListener.serverPlayerCounts.getOrDefault("minigame_dbd", 0);
        int buildsPlayers = PluginMessageListener.serverPlayerCounts.getOrDefault("builds", 0);

        // --- Ítem 1: Servidor de Minijuegos (Actualizado) ---
        ItemStack minigamesItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta minigamesMeta = minigamesItem.getItemMeta();
        minigamesMeta.setDisplayName("§bMinijuegos");
        // Actualizamos el "lore" (la descripción) para incluir el conteo de jugadores.
        minigamesMeta.setLore(Arrays.asList(
                "§7¡Haz clic para unirte a la acción!",
                "", // Línea vacía para separar
                "§eJugadores: §a" + minigamesPlayers
        ));
        minigamesMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        minigamesItem.setItemMeta(minigamesMeta);

        // --- Ítem 2: Servidor de Construcción (Actualizado) ---
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