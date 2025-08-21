package com.prax.core;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.prax.core.guis.SelectorGUI;

public class SelectorListener implements Listener {

    private final PraxCorePlugin plugin;
    private void sendPlayerToServer(Player player, String serverName) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            player.sendMessage("§cError al intentar conectarte al servidor. Por favor, inténtalo de nuevo.");
            e.printStackTrace();
        }
    }
    public SelectorListener(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    // --- MÉTODO PARA DAR LA BRÚJULA ---
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Creamos la brújula
        ItemStack compass = new ItemStack(Material.COMPASS);
        // Obtenemos sus "meta" datos para cambiarle el nombre
        ItemMeta compassMeta = compass.getItemMeta();
        compassMeta.setDisplayName("§aSelector de Modalidad"); // §a es el código para el color verde
        compass.setItemMeta(compassMeta);

        // Limpiamos el inventario del jugador y le damos la brújula en el primer slot (posición 0)
        player.getInventory().clear();
        player.getInventory().setItem(0, compass);
    }

    // --- MÉTODOS PARA BLOQUEAR LA BRÚJULA ---
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        // Verificamos si el objeto que intenta tirar es una brújula
        if (event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
            event.setCancelled(true); // Cancelamos el evento (no puede tirarla)
            event.getPlayer().sendMessage("§c¡No puedes tirar el selector de modalidad!");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        // Verificamos si el jugador está en su propio inventario y si el clic fue en el primer slot
        if (event.getSlot() == 0 && event.getWhoClicked().getInventory().equals(event.getClickedInventory())) {
            event.setCancelled(true); // Cancelamos el evento (no puede moverla)
            event.getWhoClicked().sendMessage("§c¡No puedes mover el selector de modalidad!");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack itemInHand = player.getInventory().getItemInMainHand();

        // Verificamos si el jugador hizo clic derecho (en el aire o en un bloque)
        // y si tenía una brújula en la mano.
        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) &&
                itemInHand.getType() == Material.COMPASS) {

            // Si se cumplen las condiciones, abrimos el GUI.
            new SelectorGUI(plugin).open(player);
        }
    }

    @EventHandler
    public void onSelectorGUIClick(InventoryClickEvent event) {
        // Verificamos si el inventario es nuestro selector.
        if (!event.getView().getTitle().equals("§8Selector de Servidores")) {
            return;
        }
        // Cancelamos el evento para que el jugador no pueda tomar el ítem.
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        // Verificamos que el jugador no haya hecho clic en un espacio vacío.
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }
        // Determinamos a qué servidor enviar al jugador basándonos en el ítem que ha clicado.
        switch (clickedItem.getType()) {
            case DIAMOND_SWORD:
                sendPlayerToServer(player, "minigame_dbd");
                break;
            case GRASS_BLOCK:
                sendPlayerToServer(player, "builds");
                break;
            // Puedes añadir más casos aquí para otros servidores.
            // case NETHER_STAR:
            //     sendPlayerToServer(player, "lobby1");
            //     break;
        }
    }

}