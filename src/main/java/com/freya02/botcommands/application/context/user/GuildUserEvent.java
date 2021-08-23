package com.freya02.botcommands.application.context.user;

import com.freya02.botcommands.BContext;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.commands.UserContextCommandEvent;

import javax.annotation.Nonnull;

public class GuildUserEvent extends GlobalUserEvent {
	public GuildUserEvent(BContext context, UserContextCommandEvent event) {
		super(context, event);

		if (!event.isFromGuild())
			throw new IllegalStateException("Event is not from a Guild");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <br><b>This is always true for this guild-only event</b>
	 */
	@Override
	public boolean isFromGuild() {
		return true;
	}

	/**
	 * The {@link Member} who caused this interaction.
	 * <br>This is not null as this object is not constructed if the interaction isn't in a Guild.
	 *
	 * @return The {@link Member}
	 */
	@SuppressWarnings("ConstantConditions")
	@Nonnull
	@Override
	public Member getMember() {
		return super.getMember();
	}

	/**
	 * The {@link Guild} this interaction happened in.
	 * <br>This is not null as this object is not constructed if the interaction isn't in a Guild.
	 *
	 * @return The {@link Guild}
	 */
	@SuppressWarnings("ConstantConditions")
	@Nonnull
	@Override
	public Guild getGuild() {
		return super.getGuild();
	}
}
