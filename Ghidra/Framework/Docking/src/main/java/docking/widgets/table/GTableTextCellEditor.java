/* ###
 * IP: GHIDRA
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package docking.widgets.table;

import javax.swing.*;

import docking.DockingUtils;
import docking.UndoRedoKeeper;

public class GTableTextCellEditor extends DefaultCellEditor {
	private static final Object TABLE_FOCUS_CELL_HIGHLIGHT_BORDER =
		"Table.focusCellHighlightBorder";

	private UndoRedoKeeper undoRedoKeeper;

	public GTableTextCellEditor(JTextField textField) {
		super(textField);
		setClickCountToStart(2);

		textField.setBorder(UIManager.getBorder(TABLE_FOCUS_CELL_HIGHLIGHT_BORDER));

		undoRedoKeeper = DockingUtils.installUndoRedo(textField);
	}

	@Override
	public boolean stopCellEditing() {
		if (super.stopCellEditing()) {
			undoRedoKeeper.clear();
			return true;
		}
		return false;
	}

	@Override
	public void cancelCellEditing() {
		super.cancelCellEditing();
		undoRedoKeeper.clear();
	}
}
