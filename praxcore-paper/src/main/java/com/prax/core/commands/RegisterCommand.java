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
import java.net.InetSocketAddress;

public class RegisterCommand implements CommandExecutor {

    private final PraxCorePlugin plugin;

    public RegisterCommand(PraxCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
            return true;
        }

        Player player = (Player) sender;

        if (plugin.getDataManager().isPlayerRegistered(player.getUniqueId())) {
            player.sendMessage("§c¡Ya estás registrado! Usa /login <contraseña> para entrar.");
            return true;
        }

        if (args.length != 3) {
            player.sendMessage("§cError: El uso correcto es /register <email> <contraseña> <repetir_contraseña>");
            return false;
        }

        String email = args[0];
        String password = args[1];
        String confirmPassword = args[2];

        if (!email.contains("@") || !email.contains(".")) {
            player.sendMessage("§cPor favor, introduce un correo electrónico válido.");
            return true;
        }

        if (!password.equals(confirmPassword)) {
            player.sendMessage("§cLas contraseñas no coinciden. Inténtalo de nuevo.");
            return true;
        }

        // --- LÓGICA DE DETECCIÓN DE CLIENTE BASADA EN TU IDEA ---
        InetSocketAddress address = player.getAddress();
        String ipAddress = address != null ? address.getAddress().getHostAddress() : "IP Desconocida";
        String version = String.valueOf(player.getProtocolVersion());

        // Convertimos el UUID a String para poder analizarlo.
        String playerUuidString = player.getUniqueId().toString();
        String clientType;

        // Verificamos si el UUID comienza con el prefijo estándar de Floodgate para Bedrock.
        if (playerUuidString.startsWith("00000000-0000-0000-")) {
            clientType = "BEDROCK";
        } else {
            clientType = "JAVA";
        }

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        plugin.getDataManager().registerPlayer(player.getUniqueId(), hashedPassword, email, ipAddress, clientType, version);

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = dtf.format(now);
        plugin.getDataManager().setFirstLoginDate(player.getUniqueId(), formattedDate);
        plugin.getDataManager().incrementLoginCount(player.getUniqueId());
        plugin.getDataManager().setLastLoginDate(player.getUniqueId(), formattedDate);

        player.sendMessage("§a¡Te has registrado exitosamente con el email " + email + "! Ahora, por favor, inicia sesión.");

        return true;
    }
}