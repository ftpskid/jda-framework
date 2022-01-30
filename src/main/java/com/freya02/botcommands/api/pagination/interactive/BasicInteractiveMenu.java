package com.freya02.botcommands.api.pagination.interactive;

import com.freya02.botcommands.api.components.Components;
import com.freya02.botcommands.api.components.InteractionConstraints;
import com.freya02.botcommands.api.components.builder.LambdaSelectionMenuBuilder;
import com.freya02.botcommands.api.components.event.SelectionEvent;
import com.freya02.botcommands.api.pagination.BasicPagination;
import com.freya02.botcommands.api.pagination.TimeoutInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @param <T> Type of the implementor
 */
public abstract class BasicInteractiveMenu<T extends BasicInteractiveMenu<T>> extends BasicPagination<T> {
	protected final List<InteractiveMenuItem> items;

	protected int selectedItem = 0;

	protected BasicInteractiveMenu(@NotNull List<InteractiveMenuItem> items, InteractionConstraints constraints, @Nullable TimeoutInfo<T> timeout) {
		super(constraints, timeout);

		if (items.isEmpty()) throw new IllegalStateException("No interactive menu items has been added");

		this.items = items;
	}

	@NotNull
	private SelectMenu buildSelectMenu() {
		final LambdaSelectionMenuBuilder builder = Components.selectionMenu(this::handleSelection).oneUse().setConstraints(constraints);

		final List<SelectOption> options = builder.getOptions();
		for (int i = 0, itemsSize = items.size(); i < itemsSize; i++) {
			InteractiveMenuItem item = items.get(i);

			SelectOption option = item.content().toSelectOption(String.valueOf(i));
			if (i == selectedItem) option = option.withDefault(true);

			options.add(option);
		}

		return builder.build();
	}

	private void handleSelection(SelectionEvent event) {
		selectedItem = Integer.parseInt(event.getValues().get(0));

		event.editMessage(get()).queue();
	}

	@Override
	public Message get() {
		onPreGet();

		components.addComponents(0, buildSelectMenu());

		final MessageEmbed embed = items.get(selectedItem).supplier().get(messageBuilder, components);
		messageBuilder.setEmbeds(embed);
		messageBuilder.setActionRows(components.getActionRows());

		onPostGet();

		return messageBuilder.build();
	}
}
