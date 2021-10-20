package com.freya02.bot.wiki.slash;

import com.freya02.bot.Config;
import com.freya02.botcommands.api.CommandsBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

public class SlashMain {
	public static void main(String[] args) {
		try {
			final Config config = Config.readConfig();

			final JDA jda = JDABuilder.createLight(config.getToken()).build().awaitReady();

			final CommandsBuilder builder = CommandsBuilder.newBuilder();
			builder
					.setSettingsProvider(new BasicSettingsProvider(builder.getContext()))
					.build(jda, "com.freya02.bot.wiki.slash.commands");
		} catch (Exception e) {
			e.printStackTrace();

			System.exit(-1);
		}
	}
}
