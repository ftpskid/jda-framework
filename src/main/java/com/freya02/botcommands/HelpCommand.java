package com.freya02.botcommands;

import com.freya02.botcommands.annotation.JdaCommand;
import com.freya02.botcommands.regex.MethodPattern;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;

import java.lang.reflect.Parameter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@JdaCommand(
		name = "help",
		description = "Gives help about a command",
		category = "Utils"
)
final class HelpCommand extends Command {
	private static class CommandDescription {
		private final String name, description;

		private CommandDescription(String name, String description) {
			this.name = name;
			this.description = description;
		}
	}

	private final EmbedBuilder generalHelpBuilder;

	private final Map<String, EmbedBuilder> cmdToEmbed = new HashMap<>();
	private final EmbedBuilder defaultEmbed;

	//Method can't have direct CommandInfo object as the HelpCommand is built before it is put in the command map
	private void addDetailedHelp(Command command, String name, String description, boolean addSubcommandHelp, boolean addExecutableHelp, List<CommandInfo> subcommandsInfo, List<MethodPattern> methodPatterns, String prefix) {
		final EmbedBuilder builder = new EmbedBuilder(defaultEmbed);
		final MessageEmbed.AuthorInfo author = defaultEmbed.build().getAuthor();
		if (author != null) {
			builder.setAuthor(author.getName() + " – '" + name + "' command", author.getUrl(), author.getIconUrl());
		} else {
			builder.setAuthor('\'' + name + "' command");
		}
		builder.addField("Description", description, false);

		if (addExecutableHelp) {
			for (int i = 0; i < methodPatterns.size(); i++) {
				MethodPattern methodPattern = methodPatterns.get(i);

				final StringBuilder syntax = new StringBuilder("**Syntax**: ");
				final StringBuilder example = new StringBuilder("**Example**: " + prefix + name + ' ');
				final Parameter[] parameters = methodPattern.method.getParameters();
				boolean hasEmoji = Arrays.stream(parameters).anyMatch(p -> p.getType() == Emoji.class);
				for (int j = 1; j < parameters.length; j++) {
					Parameter parameter = parameters[j];
					final Class<?> type = parameter.getType();
					final String argSyntax;
					if (type == String.class) {
						argSyntax = hasEmoji ? "\"string\"" : "string";
						example.append(hasEmoji ? "\"foo bar\"" : "foo bar");
					} else if (type == Emoji.class) {
						argSyntax = "unicode emoji/shortcode";
						example.append(":joy:");
					} else if (type == int.class || type == long.class) {
						argSyntax = "integer";
						example.append(ThreadLocalRandom.current().nextInt(100));
					} else if (type == float.class || type == double.class) {
						argSyntax = "decimal";
						example.append(ThreadLocalRandom.current().nextDouble(100));
					} else if (type == Emote.class) {
						argSyntax = "emote/emote id";
						example.append("<:kekw:673277564034482178>");
					} else if (type == Guild.class) {
						argSyntax = "guild id";
						example.append("331718482485837825");
					} else if (type == Role.class) {
						argSyntax = "role mention/role id";
						example.append("801161492296499261");
					} else if (type == User.class) {
						argSyntax = "user mention/user id";
						example.append("222046562543468545");
					} else if (type == Member.class) {
						argSyntax = "member mention/member id";
						example.append("<@222046562543468545>");
					} else if (type == TextChannel.class) {
						argSyntax = "text channel mention/text channel id";
						example.append("331718482485837825");
					} else {
						argSyntax = "?";
						example.append("?");
						System.err.println("Unknown type: " + type);
					}

					final boolean isOptional = parameter.isAnnotationPresent(com.freya02.botcommands.annotation.Optional.class);
					syntax.append(isOptional ? '[' : '`').append(argSyntax).append(isOptional ? ']' : '`').append(' ');
					example.append(' ');
				}

				if (methodPatterns.size() == 1) {
					builder.addField("Usage", syntax + "\n" + example, false);
				} else {
					builder.addField("Overload #" + (i + 1), syntax + "\n" + example, false);
				}
			}
		}

		if (addSubcommandHelp) {
			final String subcommandHelp = subcommandsInfo.stream().filter(info2 -> !info2.isHidden()).map(info2 -> "**" + info2.getName() + "** : " + info2.getDescription()).collect(Collectors.joining("\r\n"));
			builder.addField("Subcommands", subcommandHelp, false);
		}

		final Consumer<EmbedBuilder> descConsumer = command.getDetailedDescription();
		if (descConsumer != null) {
			descConsumer.accept(builder);
		}

		cmdToEmbed.put(name, builder);
	}

	public HelpCommand(Supplier<EmbedBuilder> defaultEmbedSupplier, Map<String, CommandInfo> commands, String prefix) {
		this.defaultEmbed = defaultEmbedSupplier.get();
		this.generalHelpBuilder = new EmbedBuilder(this.defaultEmbed);

		Map<String, List<CommandDescription>> categoryToDesc = new HashMap<>();
		for (CommandInfo info : Set.copyOf(commands.values())) {
			//Map category to list of commands
			if (!info.isHidden()) {
				categoryToDesc
						.computeIfAbsent(info.getCategory(), s -> new ArrayList<>())
						.add(
								new CommandDescription(info.getName(), info.getDescription())
						);
			}

			addDetailedHelp(info.getCommand(), info.getName(), info.getDescription(), info.isAddSubcommandHelp(), info.isAddExecutableHelp(), info.getSubcommandsInfo(), info.getMethodPatterns(), prefix);
		}

		boolean addedHelpEntry = false;
		for (Map.Entry<String, List<CommandDescription>> entry : categoryToDesc.entrySet()) {
			StringBuilder categoryBuilder = new StringBuilder();
			for (CommandDescription description : entry.getValue()) {
				categoryBuilder.append("**").append(description.name).append("** : ").append(description.description).append("\r\n");
			}

			if (entry.getKey().equalsIgnoreCase("utils")) {
				//Add the help entry of this one
				generalHelpBuilder.addField(entry.getKey(), categoryBuilder.toString().trim() + "\r\n**help** : Gives help about a command", false);
				addedHelpEntry = true;
			} else {
				generalHelpBuilder.addField(entry.getKey(), categoryBuilder.toString().trim(), false);
			}
		}

		if (!addedHelpEntry) {
			generalHelpBuilder.addField("Utils", "**help** : Gives help about a command", false);
		}

		//Add the precise help of this one
		addDetailedHelp(this, "help", "Gives help about a command", false, false, List.of(), List.of(), prefix);
	}

	@Override
	protected void execute(CommandEvent event) {
		if (event.hasNext(String.class)) {
			getCommandHelp(event, event.nextArgument(String.class));
		} else {
			getAllHelp(event);
		}
	}

	private synchronized void getAllHelp(BaseCommandEvent event) {
		generalHelpBuilder.setTimestamp(Instant.now());
		final Member member = event.getMember();
		generalHelpBuilder.setColor(member.getColorRaw());

		final MessageEmbed embed = generalHelpBuilder.build();
		event.getAuthor().openPrivateChannel().queue(
				privateChannel -> event.sendWithEmbedFooterIcon(privateChannel, embed, event.failureReporter("Unable to send help message")).queue(
						m -> event.reactSuccess().queue(),
						t -> event.reactError().queue()),
				t -> event.getChannel().sendMessage("Your DMs are not open").queue());

	}

	public synchronized void getCommandHelp(BaseCommandEvent event, String cmdName) {
		final CommandInfo info = event.getCommandInfo(cmdName);

		if (info == null || (info.isHidden() && !event.getOwnerIds().contains(event.getAuthor().getIdLong()))) {
			event.getChannel().sendMessage("Command '" + cmdName + "' does not exists").queue(null, event.failureReporter("Failed to send help"));
			return;
		}

		final EmbedBuilder builder = cmdToEmbed.get(cmdName);
		builder.setTimestamp(Instant.now());
		builder.setFooter("Arguments in black boxes `foo` are obligatory, but arguments in brackets [bar] are optional");

		final Member member = event.getMember();
		builder.setColor(member.getColorRaw());

		event.getChannel().sendMessage(builder.build()).queue(null, event.failureReporter("Unable to send help message"));
	}

	@Override
	public Consumer<EmbedBuilder> getDetailedDescription() {
		return builder -> {
			builder.addField("Usage :", "help [command_name]", false);
			builder.addField("Example :", "help crabrave", false);
		};
	}
}
