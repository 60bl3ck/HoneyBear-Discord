package pl.blackcode.honeybeardiscord;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class AccountStorage {

    private final Path dataDirectory;
    private final Path storagePath;
    private final Logger logger;
    private final SecureRandom random = new SecureRandom();

    private final Map<UUID, LinkedAccount> linkedAccounts = new HashMap<>();
    private final Map<UUID, PinData> pins = new HashMap<>();

    public AccountStorage(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.storagePath = dataDirectory.resolve("linked-accounts.yml");
        this.logger = logger;
    }

    public synchronized void load() {
        try {
            Files.createDirectories(dataDirectory);

            if (!Files.exists(storagePath)) {
                Files.writeString(storagePath, """
                        linked: {}
                        pins: {}
                        """);
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(storagePath)
                    .build();

            ConfigurationNode root = loader.load();

            linkedAccounts.clear();
            pins.clear();

            ConfigurationNode linkedNode = root.node("linked");

            for (Map.Entry<Object, ? extends ConfigurationNode> entry : linkedNode.childrenMap().entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey().toString());
                ConfigurationNode node = entry.getValue();

                String name = node.node("name").getString("");
                String discordId = node.node("discord-id").getString("");

                if (!discordId.isBlank()) {
                    linkedAccounts.put(uuid, new LinkedAccount(uuid, name, discordId));
                }
            }

            ConfigurationNode pinsNode = root.node("pins");

            for (Map.Entry<Object, ? extends ConfigurationNode> entry : pinsNode.childrenMap().entrySet()) {
                UUID uuid = UUID.fromString(entry.getKey().toString());
                ConfigurationNode node = entry.getValue();

                String name = node.node("name").getString("");
                String pin = node.node("pin").getString("");

                if (pin.matches("\\d{5}")) {
                    pins.put(uuid, new PinData(uuid, name, pin));
                }
            }

        } catch (Exception exception) {
            logger.error("Nie udalo sie zaladowac linked-accounts.yml.", exception);
        }
    }

    public synchronized boolean isLinked(UUID uuid) {
        return linkedAccounts.containsKey(uuid);
    }

    public synchronized boolean isDiscordLinked(String discordId) {
        return linkedAccounts.values()
                .stream()
                .anyMatch(account -> account.discordId().equals(discordId));
    }

    public synchronized String getOrCreatePin(UUID uuid, String name) {
        if (linkedAccounts.containsKey(uuid)) {
            return "";
        }

        PinData existing = pins.get(uuid);

        if (existing != null) {
            return existing.pin();
        }

        String pin = generateUniquePin();
        pins.put(uuid, new PinData(uuid, name, pin));
        save();

        return pin;
    }

    public synchronized Optional<PinData> findPin(String pin) {
        return pins.values()
                .stream()
                .filter(pinData -> pinData.pin().equals(pin))
                .findFirst();
    }

    public synchronized void linkAccount(UUID uuid, String name, String discordId) {
        linkedAccounts.put(uuid, new LinkedAccount(uuid, name, discordId));
        pins.remove(uuid);
        save();
    }

    public synchronized void reset(UUID uuid) {
        linkedAccounts.remove(uuid);
        pins.remove(uuid);
        save();
    }

    private String generateUniquePin() {
        String pin;

        do {
            pin = String.valueOf(10000 + random.nextInt(90000));
        } while (isPinUsed(pin));

        return pin;
    }

    private boolean isPinUsed(String pin) {
        return pins.values()
                .stream()
                .anyMatch(pinData -> pinData.pin().equals(pin));
    }

    private synchronized void save() {
        try {
            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                    .path(storagePath)
                    .build();

            ConfigurationNode root = loader.createNode();

            for (LinkedAccount account : linkedAccounts.values()) {
                String uuid = account.uuid().toString();

                root.node("linked", uuid, "name").set(account.name());
                root.node("linked", uuid, "discord-id").set(account.discordId());
            }

            for (PinData pinData : pins.values()) {
                String uuid = pinData.uuid().toString();

                root.node("pins", uuid, "name").set(pinData.name());
                root.node("pins", uuid, "pin").set(pinData.pin());
            }

            loader.save(root);
        } catch (IOException exception) {
            logger.error("Nie udalo sie zapisac linked-accounts.yml.", exception);
        }
    }

    public record LinkedAccount(UUID uuid, String name, String discordId) {
    }

    public record PinData(UUID uuid, String name, String pin) {
    }
}