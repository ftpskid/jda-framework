package com.freya02.botcommands.test;

import com.freya02.botcommands.api.CommandsBuilder;
import com.freya02.botcommands.api.Logging;
import com.freya02.botcommands.api.components.DefaultComponentManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.LocalDateTime;

public class SlashCommandsMain {
	private static final Logger LOGGER = Logging.getLogger();

	public static void main(@Nullable String[] args) throws NoSuchMethodException {
		try {
			final Config config = Config.readConfig();

			final JDA jda = JDABuilder.createLight(config.getToken())
					.setActivity(Activity.playing("application commands"))
					.build()
					.awaitReady();

			CommandsBuilder.newBuilder(config.getOwnerId())
					.textCommandBuilder(textCommandsBuilder -> textCommandsBuilder
							.addPrefix(config.getPrefix())
							.addFilter(messageInfo -> messageInfo.getEvent().getChannel().getIdLong() == 722891685755093076L)
					)
					.extensionsBuilder(extensionsBuilder -> extensionsBuilder
							.registerConstructorParameter(LocalDateTime.class, ignored -> LocalDateTime.now())
							.registerParameterResolver(new DateTimeResolver())
					)
					.addSearchPath("com.freya02.botcommands.test.commands")
					.setComponentManager(new DefaultComponentManager(new TestDB(config.getDbConfig()).getConnectionSupplier()))
					.setSettingsProvider(new BasicSettingsProvider())
//					.setUncaughtExceptionHandler(new ExceptionHandlerAdapter() {
//						@Override
//						public void handle(BContext context, Event event, Throwable throwable) {
//							if (event instanceof SlashCommandEvent e) {
//								e.reply("AAAAAAAAAAAAAAAAA").setEphemeral(true).queue();
//							} else {
//								System.err.println("ERR");
//							}
//						}
//
//						@Override
//						public void handle(BContext context, GuildMessageReceivedEvent event, Throwable throwable) {
//							event.getChannel().sendMessage("pc goes boom").queue();
//						}
//					})
//					.setUncaughtExceptionHandler((context, event, throwable) -> {
//						if (event instanceof SlashCommandEvent e) {
//							e.reply("AAAAAAAAAAAAAAAAA").setEphemeral(true).queue();
//						} else {
//							System.err.println("ERR");
//						}
//					})
					.build(jda);

			LOGGER.info("Finished building");
		} catch (Exception e) {
			LOGGER.error("Could not start the bot", e);

			System.exit(-2);
		}
	}
}