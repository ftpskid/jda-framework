package com.freya02.botcommands.application;

import com.freya02.botcommands.application.context.annotations.JdaMessageCommand;
import com.freya02.botcommands.application.context.annotations.JdaUserCommand;
import com.freya02.botcommands.application.context.message.GlobalMessageEvent;
import com.freya02.botcommands.application.context.message.GuildMessageEvent;
import com.freya02.botcommands.application.context.message.MessageCommandInfo;
import com.freya02.botcommands.application.context.user.GlobalUserEvent;
import com.freya02.botcommands.application.context.user.GuildUserEvent;
import com.freya02.botcommands.application.context.user.UserCommandInfo;
import com.freya02.botcommands.application.slash.ApplicationCommand;
import com.freya02.botcommands.application.slash.GlobalSlashEvent;
import com.freya02.botcommands.application.slash.GuildSlashEvent;
import com.freya02.botcommands.application.slash.SlashCommandInfo;
import com.freya02.botcommands.application.slash.annotations.JdaSlashCommand;
import com.freya02.botcommands.internal.BContextImpl;
import com.freya02.botcommands.internal.Logging;
import com.freya02.botcommands.internal.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.internal.utils.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public final class ApplicationCommandsBuilder {
	private static final Logger LOGGER = Logging.getLogger();
	private final BContextImpl context;
	private final List<Long> slashGuildIds;

	public ApplicationCommandsBuilder(@NotNull BContextImpl context, List<Long> slashGuildIds) {
		this.context = context;
		this.context.setSlashCommandsBuilder(this);
		this.slashGuildIds = slashGuildIds;
	}

	public void processApplicationCommand(ApplicationCommand applicationCommand, Method method) {
		try {
			if (!method.canAccess(applicationCommand))
				throw new IllegalStateException("Application command " + method + " is not public");
			
			if (method.isAnnotationPresent(JdaSlashCommand.class)) {
				processSlashCommand(applicationCommand, method);
			} else if (method.isAnnotationPresent(JdaUserCommand.class)) {
				processUserCommand(applicationCommand, method);
			} else if (method.isAnnotationPresent(JdaMessageCommand.class)) {
				processMessageCommand(applicationCommand, method);
			}
		} catch (Exception e) {
			throw new RuntimeException("An exception occurred while processing slash command at " + method, e);
		}
	}

	private void processUserCommand(ApplicationCommand applicationCommand, Method method) {
		if (method.getAnnotation(JdaUserCommand.class).guildOnly()) {
			if (!Utils.hasFirstParameter(method, GlobalUserEvent.class) && !Utils.hasFirstParameter(method, GuildUserEvent.class))
				throw new IllegalArgumentException("User command at " + method + " must have a GuildUserEvent or GlobalUserEvent as first parameter");

			if (!Utils.hasFirstParameter(method, GuildUserEvent.class)) {
				//If type is correct but guild specialization isn't used
				LOGGER.warn("Guild-only user command {} uses GlobalUserEvent, consider using GuildUserEvent to remove warnings related to guild stuff's nullability", method);
			}
		} else {
			if (!Utils.hasFirstParameter(method, GlobalUserEvent.class))
				throw new IllegalArgumentException("User command at " + method + " must have a GlobalUserEvent as first parameter");
		}

		final UserCommandInfo info = new UserCommandInfo(applicationCommand, method);

		LOGGER.debug("Adding user command {} for method {}", info.getName(), method);
		context.addUserCommand(info.getName(), info);
	}

	private void processMessageCommand(ApplicationCommand applicationCommand, Method method) {
		if (method.getAnnotation(JdaMessageCommand.class).guildOnly()) {
			if (!Utils.hasFirstParameter(method, GlobalMessageEvent.class) && !Utils.hasFirstParameter(method, GuildMessageEvent.class))
				throw new IllegalArgumentException("Message command at " + method + " must have a GuildMessageEvent or GlobalMessageEvent as first parameter");

			if (!Utils.hasFirstParameter(method, GuildMessageEvent.class)) {
				//If type is correct but guild specialization isn't used
				LOGGER.warn("Guild-only message command {} uses GlobalMessageEvent, consider using GuildMessageEvent to remove warnings related to guild stuff's nullability", method);
			}
		} else {
			if (!Utils.hasFirstParameter(method, GlobalMessageEvent.class))
				throw new IllegalArgumentException("Message command at " + method + " must have a GlobalMessageEvent as first parameter");
		}

		final MessageCommandInfo info = new MessageCommandInfo(applicationCommand, method);

		LOGGER.debug("Adding message command {} for method {}", info.getName(), method);
		context.addMessageCommand(info.getName(), info);
	}

	private void processSlashCommand(ApplicationCommand applicationCommand, Method method) {
		if (method.getAnnotation(JdaSlashCommand.class).guildOnly()) {
			if (!Utils.hasFirstParameter(method, GlobalSlashEvent.class) && !Utils.hasFirstParameter(method, GuildSlashEvent.class))
				throw new IllegalArgumentException("Slash command at " + method + " must have a GuildSlashEvent or GlobalSlashEvent as first parameter");

			if (!Utils.hasFirstParameter(method, GuildSlashEvent.class)) {
				//If type is correct but guild specialization isn't used
				LOGGER.warn("Guild-only slash command {} uses GlobalSlashEvent, consider using GuildSlashEvent to remove warnings related to guild stuff's nullability", method);
			}
		} else {
			if (!Utils.hasFirstParameter(method, GlobalSlashEvent.class))
				throw new IllegalArgumentException("Slash command at " + method + " must have a GlobalSlashEvent as first parameter");
		}

		final SlashCommandInfo info = new SlashCommandInfo(applicationCommand, method);

		LOGGER.debug("Adding command path {} for method {}", info.getPath(), method);
		context.addSlashCommand(info.getPath(), info);
	}

	public void postProcess() throws IOException {
		context.getJDA().setRequiredScopes("applications.commands");

		context.setApplicationCommandsCache(new ApplicationCommandsCache(context));

		final ApplicationCommandsUpdater globalUpdater = ApplicationCommandsUpdater.ofGlobal(context);
		if (globalUpdater.shouldUpdateCommands()) {
			globalUpdater.updateCommands();
			LOGGER.debug("Global commands were updated");
		} else {
			LOGGER.debug("Global commands does not have to be updated");
		}

		final List<Guild> guildCache;
		if (context.getJDA().getShardManager() != null) {
			guildCache = context.getJDA().getShardManager().getGuilds();
		} else {
			guildCache = context.getJDA().getGuilds();
		}

		tryUpdateGuildCommands(guildCache);
	}

	public boolean tryUpdateGuildCommands(Iterable<Guild> guilds) throws IOException {
		boolean changed = false;

		List<ApplicationCommandsUpdater> updaters = new ArrayList<>();
		for (Guild guild : guilds) {
			if (slashGuildIds.isEmpty() || slashGuildIds.contains(guild.getIdLong())) {
				updaters.add(ApplicationCommandsUpdater.ofGuild(context, guild));
			}
		}

		List<ImmutablePair<Guild, CompletableFuture<?>>> commandUpdatePairs = new ArrayList<>();
		for (ApplicationCommandsUpdater updater : updaters) {
			final Guild guild = updater.getGuild();

			if (updater.shouldUpdateCommands()) {
				changed = true;

				commandUpdatePairs.add(new ImmutablePair<>(guild, updater.updateCommands()));
				LOGGER.debug("Guild '{}' ({}) commands were updated", guild.getName(), guild.getId());
			} else {
				LOGGER.debug("Guild '{}' ({}) commands does not have to be updated", guild.getName(), guild.getId());
			}
		}

		final List<Long> missedGuilds = new ArrayList<>();
		for (ImmutablePair<Guild, CompletableFuture<?>> commandUpdatePair : commandUpdatePairs) {
			try {
				commandUpdatePair.getRight().join();
			} catch (CompletionException e) { // Check missing access exceptions
				if (e.getCause() instanceof ErrorResponseException) {
					if (((ErrorResponseException) e.getCause()).getErrorResponse() == ErrorResponse.MISSING_ACCESS) {
						final Guild guild = commandUpdatePair.getLeft();

						final String inviteUrl = context.getJDA().getInviteUrl() + "&guild_id=" + guild.getId();

						LOGGER.warn("Could not register guild commands for guild '{}' ({}) as it appears the OAuth2 grants misses applications.commands, you can re-invite the bot in this guild with its already existing permission with this link: {}", guild.getName(), guild.getId(), inviteUrl);
						context.getRegistrationListeners().forEach(r -> r.onGuildSlashCommandMissingAccess(guild, inviteUrl));

						missedGuilds.add(guild.getIdLong());

						continue;
					}
				}

				throw e;
			}
		}

		for (ApplicationCommandsUpdater updater : updaters) {
			final Guild guild = updater.getGuild();

			if (missedGuilds.contains(guild.getIdLong())) continue; //Missing the OAuth2 applications.commands scope in this guild

			if (updater.shouldUpdatePrivileges()) {
				changed = true;

				updater.updatePrivileges();
				LOGGER.debug("Guild '{}' ({}) commands privileges were updated", guild.getName(), guild.getId());
			} else {
				LOGGER.debug("Guild '{}' ({}) commands privileges does not have to be updated", guild.getName(), guild.getId());
			}
		}

		return changed;
	}
}