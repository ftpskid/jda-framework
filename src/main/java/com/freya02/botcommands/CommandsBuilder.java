package com.freya02.botcommands;

import com.freya02.botcommands.annotation.RequireOwner;
import com.freya02.botcommands.buttons.ButtonsBuilder;
import com.freya02.botcommands.buttons.KeyProvider;
import com.freya02.botcommands.prefixed.*;
import com.freya02.botcommands.prefixed.annotation.AddExecutableHelp;
import com.freya02.botcommands.prefixed.annotation.AddSubcommandHelp;
import com.freya02.botcommands.prefixed.annotation.JdaCommand;
import com.freya02.botcommands.slash.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class CommandsBuilder {
	private static final Logger LOGGER = Logging.getLogger();

	private final BContextImpl context = new BContextImpl();
	private final PrefixedCommandsBuilder prefixedCommandsBuilder = new PrefixedCommandsBuilder(context);
	private final SlashCommandsBuilder slashCommandsBuilder = new SlashCommandsBuilder(context);
	private final ButtonsBuilder buttonsBuilder = new ButtonsBuilder(context);

	private boolean disableHelpCommand;
	private boolean disableSlashHelpCommand;
	private List<Long> slashGuildIds = null;

	private CommandsBuilder(@NotNull String prefix, long topOwnerId) {
		Utils.requireNonBlank(prefix, "Prefix");
		context.setPrefixes(List.of(prefix));
		context.addOwner(topOwnerId);
	}

	private CommandsBuilder(long topOwnerId) {
		context.addOwner(topOwnerId);
	}

	/**
	 * Constructs a new instance of {@linkplain CommandsBuilder} with ping-as-prefix enabled
	 *
	 * @param topOwnerId The most owner of the bot
	 */
	public static CommandsBuilder withPing(long topOwnerId) {
		return new CommandsBuilder(topOwnerId);
	}

	/**
	 * Constructs a new instance of {@linkplain CommandsBuilder}
	 *
	 * @param prefix     Prefix of the bot
	 * @param topOwnerId The most owner of the bot
	 */
	public static CommandsBuilder withPrefix(@NotNull String prefix, long topOwnerId) {
		return new CommandsBuilder(prefix, topOwnerId);
	}

	/**
	 * Allows to change the framework's default messages while keeping the builder pattern
	 *
	 * @param modifier Consumer to change the default messages
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder overrideMessages(Consumer<DefaultMessages> modifier) {
		modifier.accept(context.getDefaultMessages());

		return this;
	}

	/**
	 * Enables {@linkplain AddSubcommandHelp} on all registered commands
	 *
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder addSubcommandHelpByDefault() {
		context.setAddSubcommandHelpByDefault(true);

		return this;
	}

	/**
	 * Enables {@linkplain AddExecutableHelp} on all registered commands
	 *
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder addExecutableHelpByDefault() {
		context.setAddExecutableHelpByDefault(true);

		return this;
	}

	/**
	 * Disables the help command for prefixed commands and replaces the implementation when incorrect syntax is detected
	 *
	 * @param helpConsumer Consumer used to show help when a command is detected but their syntax is invalid
	 *
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder disableHelpCommand(Consumer<BaseCommandEvent> helpConsumer) {
		this.disableHelpCommand = true;
		this.context.overrideHelp(helpConsumer);

		return this;
	}

	/**
	 * Disables the /help command for slash commands
	 *
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder disableSlashHelpCommand() {
		this.disableSlashHelpCommand = true;

		return this;
	}

	/**
	 * Debug feature - Makes it so slash commands are only updated on these guilds
	 *
	 * @param slashGuildIds IDs of the guilds
	 * @return This builder for chaining convenience
	 */
	public CommandsBuilder updateCommandsOnGuildIds(List<Long> slashGuildIds) {
		this.slashGuildIds = slashGuildIds;

		return this;
	}

	//TODO
	public CommandsBuilder setKeyProvider(KeyProvider provider) {
		context.setKeyProvider(provider);

		return this;
	}

	/**
	 * <p>Sets the embed builder and the footer icon that this library will use as base embed builder</p>
	 * <p><b>Note : The icon name when used will be "icon.jpg", your icon must be a JPG file and be the same name</b></p>
	 *
	 * @param defaultEmbedFunction      The default embed builder
	 * @param defaultFooterIconSupplier The default icon for the footer
	 * @return This builder
	 */
	public CommandsBuilder setDefaultEmbedFunction(@NotNull Supplier<EmbedBuilder> defaultEmbedFunction, @NotNull Supplier<InputStream> defaultFooterIconSupplier) {
		this.context.setDefaultEmbedSupplier(defaultEmbedFunction);
		this.context.setDefaultFooterIconSupplier(defaultFooterIconSupplier);
		return this;
	}

	/**
	 * Adds owners, they can access the commands annotated with {@linkplain RequireOwner}
	 *
	 * @param ownerIds Owners Long IDs to add
	 * @return This builder
	 */
	public CommandsBuilder addOwners(long... ownerIds) {
		for (long ownerId : ownerIds) {
			context.addOwner(ownerId);
		}

		return this;
	}

	private void buildClasses(List<Class<?>> classes) {
		try {
			for (Class<?> aClass : classes) {
				processClass(aClass);
			}

			if (!disableHelpCommand) {
				processClass(HelpCommand.class);

				final HelpCommand help = (HelpCommand) context.findCommand("help");
				if (help == null) throw new IllegalStateException("HelpCommand did not build properly");
				help.generate();
			}

			if (!disableSlashHelpCommand) {
				processClass(SlashHelpCommand.class);

				final SlashCommandInfo info = context.findSlashCommand("help");
				if (info == null) throw new IllegalStateException("SlashHelpCommand did not build properly");

				((SlashHelpCommand) info.getInstance()).generate();
			}

			//Load button listeners
			for (Class<?> aClass : classes) {
				buttonsBuilder.processButtonListener(aClass);
			}

			LOGGER.info("Loaded {} commands", context.getCommands().size());
			printCommands(context.getCommands(), 0);

			LOGGER.info("Loaded {} slash commands", context.getSlashCommands().size());
			printSlashCommands(context.getSlashCommands());

			slashCommandsBuilder.postProcess(slashGuildIds);

			buttonsBuilder.postProcess();

			LOGGER.info("Finished registering all commands");
		} catch (Throwable e) {
			if (LOGGER.isErrorEnabled()) {
				LOGGER.error("An error occured while loading the commands, the commands will not work");
			} else { //Dont want this error hidden by the lack of logging framework
				System.err.println("An error occured while loading the commands, the commands will not work");
			}

			throw new RuntimeException(e);
		}
	}

	private void printCommands(Collection<Command> commands, int indent) {
		for (Command command : commands) {
			LOGGER.debug("{}- '{}' Bot permission=[{}] User permissions=[{}]",
					"\t".repeat(indent),
					command.getInfo().getName(),
					command.getInfo().getBotPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")),
					command.getInfo().getUserPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")));

			printCommands(command.getInfo().getSubcommands(), indent + 1);
		}
	}

	private void printSlashCommands(Collection<SlashCommandInfo> commands) {
		for (SlashCommandInfo command : commands) {
			LOGGER.debug("{} - '{}' Bot permission=[{}] User permissions=[{}]",
					command.isGuildOnly() ? "Guild    " : "Guild+DMs",
					command.getPath(),
					command.getBotPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")),
					command.getUserPermissions().stream().map(Permission::getName).collect(Collectors.joining(", ")));
		}
	}

	private void processClass(Class<?> aClass) throws InvocationTargetException, IllegalAccessException, InstantiationException, NoSuchMethodException {
		if (isCommand(aClass) && aClass.getDeclaringClass() == null) { //Declaring class returns null for anonymous classes, we only need to check if the class is not an inner class
			boolean isInstantiable = Utils.isInstantiable(aClass);

			if (isInstantiable) {
				Object someCommand;

				if (Command.class.isAssignableFrom(aClass)) {
					final Constructor<?> constructor = aClass.getDeclaredConstructor(BContext.class);
					if (!constructor.canAccess(null))
						throw new IllegalStateException("Constructor " + constructor + " is not public");

					someCommand = constructor.newInstance(context);
				} else { //Slash command
					try {
						final Constructor<?> constructor = aClass.getDeclaredConstructor();
						if (!constructor.canAccess(null))
							throw new IllegalStateException("Constructor " + constructor + " is not public");

						someCommand = constructor.newInstance();
					} catch (NoSuchMethodException ignored) {
						final Constructor<?> constructor = aClass.getDeclaredConstructor(BContext.class);
						if (!constructor.canAccess(null))
							throw new IllegalStateException("Constructor " + constructor + " is not public");

						someCommand = constructor.newInstance(context);
					}
				}

				context.getClassToObjMap().put(aClass, someCommand);

				if (someCommand instanceof Command) {
					prefixedCommandsBuilder.processPrefixedCommand((Command) someCommand);
				} else if (someCommand instanceof SlashCommand) {
					slashCommandsBuilder.processSlashCommand((SlashCommand) someCommand);
				} else {
					throw new IllegalArgumentException("How is that a command " + someCommand.getClass().getName() + " ???");
				}
			}
		}
	}

	private boolean isCommand(Class<?> aClass) {
		if (Modifier.isAbstract(aClass.getModifiers()))
			return false;

		if (SlashCommand.class.isAssignableFrom(aClass))
			return true;

		return Command.class.isAssignableFrom(aClass) && aClass.isAnnotationPresent(JdaCommand.class);
	}

	/**
	 * Builds the command listener and automatically registers all listener to the JDA instance
	 *
	 * @param jda                The JDA instance of your bot
	 * @param commandPackageName The package name where all the commands are, ex: com.freya02.commands
	 * @throws IOException If an exception occurs when reading the jar path or getting classes
	 */
	public void build(JDA jda, @NotNull String commandPackageName) throws IOException {
		Utils.requireNonBlank(commandPackageName, "Command package");

		setupContext(jda);

		final Class<?> callerClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
		buildClasses(Utils.getClasses(IOUtils.getJarPath(callerClass), commandPackageName, 3));

		EventWaiter.createWaiter(context);

		jda.addEventListener(new CommandListener(context), new SlashCommandListener(context));
	}

	private void setupContext(JDA jda) {
		context.setJda(jda);
		if (context.getPrefixes().isEmpty()) {
			context.setPrefixes(List.of("<@" + jda.getSelfUser().getId() + "> ", "<@!" + jda.getSelfUser().getId() + "> "));
		}
	}

	public BContext getContext() {
		return context;
	}
}