/*
 * WiFiKeyShare. Share Wi-Fi passwords with QR codes or NFC tags.
 * Copyright (C) 2016 Bruno Parmentier <dev@brunoparmentier.be>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.wifikeyshare.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.View;

/**
 * RecyclerView with ContextMenuInfo support
 * Source: https://gist.github.com/Teovald/cba0aa150e60b727636d#file-contextmenurecyclerview-java
 */
public class ContextMenuRecyclerView extends RecyclerView {

    private ContextMenu.ContextMenuInfo contextMenuInfo;

    public ContextMenuRecyclerView(Context context) {
        super(context);
    }

    public ContextMenuRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContextMenuRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return contextMenuInfo;
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        final int longPressPosition = getChildLayoutPosition(originalView);
        if (longPressPosition >= 0) {
            final long longPressId = getAdapter().getItemId(longPressPosition);
            contextMenuInfo = new RecyclerContextMenuInfo(longPressPosition, longPressId);
            return super.showContextMenuForChild(originalView);
        }
        return false;

    }

    /**
     * Extra menu information provided to the
     * {@link android.view.View.OnCreateContextMenuListener#onCreateContextMenu(ContextMenu, View, ContextMenu.ContextMenuInfo)}
     * callback when a context menu is brought up for this AdapterView.
     */
    public static class RecyclerContextMenuInfo implements ContextMenu.ContextMenuInfo {

        /**
         * The position in the adapter for which the context menu is being
         * displayed.
         */
        public int position;
        /**
         * The row id of the item for which the context menu is being displayed.
         */
        public long id;

        public RecyclerContextMenuInfo(int position, long id) {
            this.position = position;
            this.id = id;
        }
    }
}
