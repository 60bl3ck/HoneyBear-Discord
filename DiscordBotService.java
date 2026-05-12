package pl.blackcode.honeybeardiscord;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.awt.Color;
import java.util.Optional;

public final class DiscordBotService extends ListenerAdapter {

    private static final String VERIFY_BUTTON_ID = "honeybear_discord_verify_button";
    private static final String VERIFY_MODAL_ID = "honeybear_discord_verify_modal";
    private static final String PIN_INPUT_ID = "pin";

    private final HoneyBearDiscordPlugin plugin;
    private final Logger logger;

    private JDA jda;

    public DiscordBotService(HoneyBearDiscordPlugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void start() {
        String token = plugin.getConfig().getBotToken();

        if (token == null || token.isBlank() || token.equalsIgnoreCase("WKLEJ_NOWY_TOKEN_BOTA") || token.equalsIgnoreCase("WKLEJ_TOKEN_BOTA")) {
            logger.warn("Token bota Discord nie jest ustawiony. Bot nie zostal wlaczony.");
            return;
        }

        try {
            this.jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .build();

            logger.info("Bot Discord jest uruchamiany...");
        } catch (Exception exception) {
            logger.error("Nie udalo sie uruchomic bota Discord.", exception);
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdownNow();
        }
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        ensureVerifyMessage();
    }

    private void ensureVerifyMessage() {
        String channelId = plugin.getConfig().getChannelId();

        if (channelId == null || channelId.isBlank() || channelId.equalsIgnoreCase("ID_KANALU")) {
            logger.warn("channel-id w config.yml nie jest ustawione.");
            return;
        }

        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel == null) {
            logger.warn("Nie znaleziono kanalu Discord o ID: {}", channelId);
            return;
        }

        String messageId = plugin.getConfig().getVerifyMessageId();

        if (messageId != null && !messageId.isBlank()) {
            channel.retrieveMessageById(messageId).queue(
                    message -> message.editMessageEmbeds(createVerifyEmbed().build())
                            .setComponents(ActionRow.of(Button.primary(
                                    VERIFY_BUTTON_ID,
                                    plugin.getConfig().getVerifyButtonLabel()
                            )))
                            .queue(
                                    success -> logger.info("Stala wiadomosc Discord zostala zaktualizowana."),
                                    error -> sendNewVerifyMessage(channel)
                            ),
                    error -> sendNewVerifyMessage(channel)
            );
            return;
        }

        sendNewVerifyMessage(channel);
    }

    private void sendNewVerifyMessage(TextChannel channel) {
        channel.sendMessageEmbeds(createVerifyEmbed().build())
                .setComponents(ActionRow.of(Button.primary(
                        VERIFY_BUTTON_ID,
                        plugin.getConfig().getVerifyButtonLabel()
                )))
                .queue(message -> {
                    plugin.getConfig().setVerifyMessageId(message.getId());
                    logger.info("Utworzono nowa stala wiadomosc Discord. ID: {}", message.getId());
                }, error -> logger.error("Nie udalo sie wyslac stalej wiadomosci Discord.", error));
    }

    private EmbedBuilder createVerifyEmbed() {
        return new EmbedBuilder()
                .setTitle(plugin.getConfig().getVerifyTitle())
                .setDescription(plugin.getConfig().getVerifyDescription())
                .setColor(parseColor(plugin.getConfig().getVerifyColor()));
    }

    private Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return new Color(200, 155, 60);
        }

        String fixed = value.replace("#", "");

        try {
            return new Color(Integer.parseInt(fixed, 16));
        } catch (Exception ignored) {
            return new Color(200, 155, 60);
        }
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.getComponentId().equals(VERIFY_BUTTON_ID)) {
            return;
        }

        TextInput pinInput = TextInput.create(
                        PIN_INPUT_ID,
                        plugin.getConfig().getModalInputLabel(),
                        TextInputStyle.SHORT
                )
                .setRequired(true)
                .setRequiredRange(5, 5)
                .setPlaceholder("12345")
                .build();

        Modal modal = Modal.create(VERIFY_MODAL_ID, plugin.getConfig().getModalTitle())
                .addActionRow(pinInput)
                .build();

        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals(VERIFY_MODAL_ID)) {
            return;
        }

        String pin = event.getValue(PIN_INPUT_ID) == null
                ? ""
                : event.getValue(PIN_INPUT_ID).getAsString().trim();

        if (!pin.matches("\\d{5}")) {
            event.reply(plugin.getConfig().getBotInvalidPin())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (plugin.getAccountStorage().isDiscordLinked(event.getUser().getId())) {
            event.reply(plugin.getConfig().getBotAlreadyLinkedDiscord())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        Optional<AccountStorage.PinData> pinDataOptional = plugin.getAccountStorage().findPin(pin);

        if (pinDataOptional.isEmpty()) {
            event.reply(plugin.getConfig().getBotInvalidPin())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        AccountStorage.PinData pinData = pinDataOptional.get();

        plugin.getAccountStorage().linkAccount(
                pinData.uuid(),
                pinData.name(),
                event.getUser().getId()
        );

        plugin.getProxyServer().getPlayer(pinData.uuid()).ifPresent(player ->
                player.sendMessage(TextUtil.color(plugin.getConfig().getLinkedSuccessGame()))
        );

        giveVerifyRole(event);

        event.reply(plugin.getConfig().getBotLinkedSuccess()
                        .replace("%player%", pinData.name()))
                .setEphemeral(true)
                .queue();
    }

    private void giveVerifyRole(ModalInteractionEvent event) {
        String guildId = plugin.getConfig().getGuildId();
        String roleId = plugin.getConfig().getVerifyRoleId();

        if (guildId == null || guildId.isBlank() || roleId == null || roleId.isBlank()) {
            logger.warn("Nie ustawiono guild-id lub verify-role-id w config.yml. Rola nie zostala nadana.");
            return;
        }

        Guild guild = jda.getGuildById(guildId);

        if (guild == null) {
            logger.warn("Nie znaleziono serwera Discord o ID: {}", guildId);
            return;
        }

        Role role = guild.getRoleById(roleId);

        if (role == null) {
            logger.warn("Nie znaleziono roli Discord o ID: {}", roleId);
            return;
        }

        guild.retrieveMemberById(event.getUser().getId()).queue(member ->
                        guild.addRoleToMember(member, role).queue(
                                success -> logger.info("Nadano role {} uzytkownikowi {}.", roleId, event.getUser().getId()),
                                error -> logger.warn("Nie udalo sie nadac roli {} uzytkownikowi {}. Sprawdz permisje bota i hierarchie rol.", roleId, event.getUser().getId(), error)
                        ),
                error -> logger.warn("Nie udalo sie pobrac uzytkownika {} z serwera Discord.", event.getUser().getId(), error)
        );
    }
}