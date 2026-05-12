package pl.blackcode.honeybeardiscord;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class DiscordCommand implements SimpleCommand {

    private static final String ADMIN_PERMISSION = "honeybear.discord.*";

    private final ProxyServer proxyServer;
    private final HoneyBearDiscordPlugin plugin;

    public DiscordCommand(ProxyServer proxyServer, HoneyBearDiscordPlugin plugin) {
        this.proxyServer = proxyServer;
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(TextUtil.color(plugin.getConfig().getConsoleDenied()));
            return;
        }

        if (args.length == 0) {
            sendDiscordInfo(player);
            return;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            resetPlayer(player, args[1]);
            return;
        }

        if (player.hasPermission(ADMIN_PERMISSION)) {
            player.sendMessage(TextUtil.color(plugin.getConfig().getUsageAdmin()));
        } else {
            player.sendMessage(TextUtil.color(plugin.getConfig().getNoPermission()));
        }
    }

    private void sendDiscordInfo(Player player) {
        player.sendMessage(TextUtil.clickable(
                plugin.getConfig().getDiscordLink(),
                plugin.getConfig().getDiscordHover(),
                plugin.getConfig().getInviteLink()
        ));

        if (plugin.getAccountStorage().isLinked(player.getUniqueId())) {
            return;
        }

        String pin = plugin.getAccountStorage().getOrCreatePin(
                player.getUniqueId(),
                player.getUsername()
        );

        for (String line : plugin.getConfig().getPinMessage()) {
            player.sendMessage(TextUtil.color(
                    line,
                    Map.of("%pin%", pin)
            ));
        }
    }

    private void resetPlayer(Player admin, String targetName) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            admin.sendMessage(TextUtil.color(plugin.getConfig().getNoPermission()));
            return;
        }

        Optional<Player> targetOptional = proxyServer.getPlayer(targetName);
        if (targetOptional.isEmpty()) {
            admin.sendMessage(TextUtil.color(
                    plugin.getConfig().getPlayerNotFound(),
                    Map.of("%player%", targetName)
            ));
            return;
        }

        Player target = targetOptional.get();

        plugin.getAccountStorage().reset(target.getUniqueId());

        admin.sendMessage(TextUtil.color(
                plugin.getConfig().getResetSuccess(),
                Map.of("%player%", target.getUsername())
        ));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            return List.of();
        }

        if (!player.hasPermission(ADMIN_PERMISSION)) {
            return List.of();
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            return List.of("reset");
        }

        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ROOT);
            if ("reset".startsWith(current)) {
                return List.of("reset");
            }

            return List.of();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            String current = args[1].toLowerCase(Locale.ROOT);
            List<String> suggestions = new ArrayList<>();

            for (Player onlinePlayer : proxyServer.getAllPlayers()) {
                String username = onlinePlayer.getUsername();

                if (username.toLowerCase(Locale.ROOT).startsWith(current)) {
                    suggestions.add(username);
                }
            }

            return suggestions;
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}