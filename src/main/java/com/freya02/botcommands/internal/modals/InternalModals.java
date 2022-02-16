package com.freya02.botcommands.internal.modals;

import com.freya02.botcommands.api.modals.ModalBuilder;
import com.freya02.botcommands.api.modals.TextInputBuilder;
import com.freya02.botcommands.internal.BContextImpl;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import org.jetbrains.annotations.NotNull;

public class InternalModals {
	private static BContextImpl context;

	private InternalModals() {}

	static void setContext(BContextImpl context) {
		InternalModals.context = context;
	}

	private static BContextImpl getContext() {
		if (context == null)
			throw new IllegalStateException("Cannot use modals before the framework is initialized");

		return context;
	}

	@NotNull
	public static ModalBuilder create(@NotNull String title) {
		return new ModalBuilder(getContext().getModalMaps(), title);
	}

	@NotNull
	public static TextInputBuilder createTextInput(@NotNull String label, @NotNull TextInputStyle style) {
		return new TextInputBuilder(getContext().getModalMaps(), label, style);
	}
}
