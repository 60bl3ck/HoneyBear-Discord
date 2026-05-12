package pl.blackcode.honeybeardiscord;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "honeybear-discord",
        name = "HoneyBear-Discord",
        version = "1.0-SNAPSHOT",
        authors = {"60bl3ck"}
)
public final class HoneyBearDiscordPlugin {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Path dataDirectory;

    private PluginConfig config;
    private AccountStorage accountStorage;
    private DiscordBotService discordBotService;

    @Inject
    public HoneyBearDiscordPlugin(
            ProxyServer proxyServer,
            Logger logger,
            @DataDirectory Path dataDirectory
    ) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        this.config = new PluginConfig(dataDirectory, logger);
        this.config.load();

        this.accountStorage = new AccountStorage(dataDirectory, logger);
        this.accountStorage.load();

        CommandManager commandManager = proxyServer.getCommandManager();

        CommandMeta commandMeta = commandManager.metaBuilder("discord")
                .plugin(this)
                .build();

        commandManager.register(commandMeta, new DiscordCommand(proxyServer, this));

        this.discordBotService = new DiscordBotService(this, logger);
        this.discordBotService.start();

        logger.info("HoneyBear-Discord zostal wlaczony.");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (discordBotService != null) {
            discordBotService.shutdown();
        }
    }

    public ProxyServer getProxyServer() {
        return proxyServer;
    }

    public PluginConfig getConfig() {
        return config;
    }

    public AccountStorage getAccountStorage() {
        return accountStorage;
    }

    public void reloadConfig() {
        config.load();
    }
}