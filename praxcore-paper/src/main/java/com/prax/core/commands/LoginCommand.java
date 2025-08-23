// Ubicación: praxcore-paper/src/main/java/com/prax/core/commands/LoginCommand.java
package com.prax.core.commands;

import com.prax.core.PraxCorePlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoginCommand implements CommandExecutor {

    private final PraxCorePlugin plugin;

    public LoginCommand(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;

        if (!plugin.getDataManager().isPlayerRegistered(player.getUniqueId())) {
            player.sendMessage("§cNo estás registrado. Usa /register <email> <contraseña> <contraseña> para crear una cuenta.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("§cError: El uso correcto es /login <contraseña>");
            return false;
        }

        String password = args[0];
        String hashedPassword = plugin.getDataManager().getPasswordHash(player.getUniqueId());

        if (BCrypt.checkpw(password, hashedPassword)) {
            // Verificar si ya está autenticado
            if (plugin.isAuthenticated(player.getUniqueId())) {
                player.sendMessage("§e¡Ya estás autenticado!");
                return true;
            }

            plugin.setAuthenticated(player.getUniqueId(), true);
            plugin.sendCreateSessionMessage(player);
            plugin.setLoginTime(player.getUniqueId());

            player.sendMessage("§a¡Has iniciado sesión correctamente!");

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            String formattedDate = dtf.format(now);

            plugin.getDataManager().incrementLoginCount(player.getUniqueId());
            plugin.getDataManager().setLastLoginDate(player.getUniqueId(), formattedDate);
        } else {
            player.sendMessage("§cContraseña incorrecta. Inténtalo de nuevo.");
        }

        return true;
    }
}