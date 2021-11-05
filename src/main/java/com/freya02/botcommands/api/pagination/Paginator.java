package com.freya02.botcommands.api.pagination;

import com.freya02.botcommands.api.BContext;
import com.freya02.botcommands.api.pagination.menu.ButtonContent;
import com.freya02.botcommands.api.pagination.menu2.Menu;

/**
 * Provides a paginator
 * <br>You provide the pages, it displays them one by one.
 * <br>Initial page is page 0, there is navigation buttons and an optional delete button
 * <br><b>The delete button cannot be used if the message is ephemeral</b>
 *
 * <br><br>
 * <b>The button IDs used by this paginator and those registered by the {@link PaginatorComponents} in the {@link PaginatorSupplier} are cleaned up once the embed is removed with the button</b>
 * <br>When the message is deleted, you would also have to call {@link #cleanup(BContext)}
 *
 * @see Menu
 */
public final class Paginator extends BasicPaginator<Paginator> {
	Paginator(long ownerId, TimeoutInfo<Paginator> timeout, int _maxPages, PaginatorSupplier supplier, boolean hasDeleteButton, ButtonContent firstContent, ButtonContent previousContent, ButtonContent nextContent, ButtonContent lastContent, ButtonContent deleteContent) {
		super(ownerId, timeout, _maxPages, supplier, hasDeleteButton, firstContent, previousContent, nextContent, lastContent, deleteContent);
	}
}
