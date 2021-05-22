package com.freya02.botcommands.parameters;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class StringResolver extends ParameterResolver {
	@Override
	public boolean isRegexCommandSupported() {
		return true;
	}

	@Override
	public Object resolve(GuildMessageReceivedEvent event, String[] args) {
		return args[0];
	}

	@Override
	public boolean isSlashCommandSupported() {
		return true;
	}

	@Override
	public Object resolve(SlashCommandEvent event, OptionMapping optionData) {
		return optionData.getAsString();
	}

	@Override
	public boolean isButtonSupported() {
		return true;
	}

	@Override
	public Object resolve(ButtonClickEvent event, String arg) {
		return arg;
	}
}
