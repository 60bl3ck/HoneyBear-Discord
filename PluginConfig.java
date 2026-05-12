package pl.blackcode.honeybeardiscord;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PluginConfig {

    private final Path dataDirectory;
    private final Path configPath;
    private final Logger logger;

    private YamlConfigurationLoader loader;
    private ConfigurationNode root;

    private String botToken;
    private String guildId;
    private String channelId;
    private String inviteLink;
    private String verifyMessageId;
    private String verifyRoleId;

    private String verifyTitle;
    private String verifyDescription;
    private String verifyColor;
    private String verifyButtonLabel;
    private String modalTitle;
    private String modalInputLabel;

    private String botLinkedSuccess;
    private String botInvalidPin;
    private String botAlreadyLinkedDiscord;

    private String prefix;
    private String discordLink;
    private String discordHover;
    private List<String> pinMessage;
    private String linkedSuccessGame;
    private String resetSuccess;
    private String noPermission;
    private String consoleDenied;
    private String playerNotFound;
    private String usageAdmin;

    public PluginConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.configPath = dataDirectory.resolve("config.yml");
        this.logger = logger;
    }

    public void load() {
        try {
            createDefaultConfig();

            this.loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();

            this.root = loader.load();

            this.botToken = root.node("discord", "token").getString("WKLEJ_NOWY_TOKEN_BOTA");
            this.guildId = root.node("discord", "guild-id").getString("1503521317931192380");
            this.channelId = root.node("discord", "channel-id").getString("1503521371404112003");
            this.inviteLink = root.node("discord", "invite-link").getString("https://discord.gg/ad4GWTZtuD");
            this.verifyMessageId = root.node("discord", "verify-message-id").getString("1503524051476545627");
            this.verifyRoleId = root.node("discord", "verify-role-id").getString("1472564608974983300");

            this.verifyTitle = root.node("discord", "verify-message", "title").getString("HoneyBear");
            this.verifyDescription = root.node("discord", "verify-message", "description").getString("Połącz swoje konto Minecraft z Discordem i odbierz dostęp do społeczności.");
            this.verifyColor = root.node("discord", "verify-message", "color").getString("#c89b3c");
            this.verifyButtonLabel = root.node("discord", "verify-message", "button-label").getString("Połącz konto");
            this.modalTitle = root.node("discord", "verify-message", "modal-title").getString("Weryfikacja konta");
            this.modalInputLabel = root.node("discord", "verify-message", "modal-input-label").getString("Wpisz PIN z gry");

            this.botLinkedSuccess = root.node("discord", "bot-messages", "linked-success").getString("✅ Konto zostało połączone z graczem **%player%**.");
            this.botInvalidPin = root.node("discord", "bot-messages", "invalid-pin").getString("❌ Podany PIN jest nieprawidłowy albo został już użyty.");
            this.botAlreadyLinkedDiscord = root.node("discord", "bot-messages", "already-linked-discord").getString("❌ To konto Discord jest już połączone z innym kontem Minecraft.");

            this.prefix = root.node("messages", "prefix").getString("");

            this.discordLink = root.node("messages", "discord-link").getString("&#3355ff&lDISCORD &#8f9bb3• &#d7dde8Kliknij &#c89b3c&lTUTAJ &#d7dde8aby dołączyć do serwera");
            this.discordHover = root.node("messages", "discord-hover").getString("&#c89b3cKliknij, aby otworzyć zaproszenie Discord.");

            this.pinMessage = root.node("messages", "pin-message").getList(String.class, List.of(
                    "&#3355ff&lWERYFIKACJA &#8f9bb3• &#d7dde8Twój kod PIN: &#c89b3c&l%pin%",
                    "&#9aa4b2Użyj tego kodu w wiadomości weryfikacyjnej na Discordzie."
            ));

            this.linkedSuccessGame = root.node("messages", "linked-success-game").getString("&#3355ff&lDISCORD &#8f9bb3• &#7fb069Pomyślnie połączono Twoje konto z Discordem.");

            this.resetSuccess = root.node("messages", "reset-success").getString("&#7fb069Połączenie discorda z graczem &#c89b3c%player% &#7fb069zostało zresetowane.");
            this.noPermission = root.node("messages", "no-permission").getString("&#c75c5cNie posiadasz uprawnień do tej komendy.");
            this.consoleDenied = root.node("messages", "console-denied").getString("Ta komenda jest dostępna tylko dla gracza.");
            this.playerNotFound = root.node("messages", "player-not-found").getString("&#c75c5cNie znaleziono gracza &#c89b3c%player%&#c75c5c.");
            this.usageAdmin = root.node("messages", "usage-admin").getString("&#c89b3cUżycie: &#d7dde8/discord &#8f9bb3lub &#d7dde8/discord reset <nick>");

        } catch (Exception exception) {
            logger.error("Nie udalo sie zaladowac config.yml pluginu HoneyBear-Discord.", exception);
        }
    }

    private void createDefaultConfig() throws IOException {
        if (Files.exists(configPath)) {
            return;
        }

        Files.createDirectories(dataDirectory);

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.yml")) {
            if (inputStream == null) {
                throw new IOException("Brak config.yml w resources.");
            }

            Files.copy(inputStream, configPath);
        }
    }

    public void setVerifyMessageId(String messageId) {
        try {
            this.verifyMessageId = messageId;
            this.root.node("discord", "verify-message-id").set(messageId);
            this.loader.save(root);
        } catch (Exception exception) {
            logger.error("Nie udalo sie zapisac verify-message-id do config.yml.", exception);
        }
    }

    public String getBotToken() {
        return botToken;
    }

    public String getGuildId() {
        return guildId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getInviteLink() {
        return inviteLink;
    }

    public String getVerifyMessageId() {
        return verifyMessageId;
    }

    public String getVerifyRoleId() {
        return verifyRoleId;
    }

    public String getVerifyTitle() {
        return verifyTitle;
    }

    public String getVerifyDescription() {
        return verifyDescription;
    }

    public String getVerifyColor() {
        return verifyColor;
    }

    public String getVerifyButtonLabel() {
        return verifyButtonLabel;
    }

    public String getModalTitle() {
        return modalTitle;
    }

    public String getModalInputLabel() {
        return modalInputLabel;
    }

    public String getBotLinkedSuccess() {
        return botLinkedSuccess;
    }

    public String getBotInvalidPin() {
        return botInvalidPin;
    }

    public String getBotAlreadyLinkedDiscord() {
        return botAlreadyLinkedDiscord;
    }

    public String getDiscordLink() {
        return replacePrefix(discordLink);
    }

    public String getDiscordHover() {
        return replacePrefix(discordHover);
    }

    public List<String> getPinMessage() {
        return pinMessage.stream()
                .map(this::replacePrefix)
                .toList();
    }

    public String getLinkedSuccessGame() {
        return replacePrefix(linkedSuccessGame);
    }

    public String getResetSuccess() {
        return replacePrefix(resetSuccess);
    }

    public String getNoPermission() {
        return replacePrefix(noPermission);
    }

    public String getConsoleDenied() {
        return replacePrefix(consoleDenied);
    }

    public String getPlayerNotFound() {
        return replacePrefix(playerNotFound);
    }

    public String getUsageAdmin() {
        return replacePrefix(usageAdmin);
    }

    private String replacePrefix(String message) {
        if (message == null) {
            return "";
        }

        return message.replace("%prefix%", prefix == null ? "" : prefix);
    }
}