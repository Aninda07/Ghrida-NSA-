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
package docking.action.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.Icon;

import docking.ActionContext;
import docking.menu.ActionState;
import docking.menu.MultiStateDockingAction;
import docking.widgets.EventTrigger;

/**
 * Builder for {@link MultiStateDockingAction}
 * 
 * @param <T> The action state type
 */
public class MultiStateActionBuilder<T> extends
		AbstractActionBuilder<MultiStateDockingAction<T>, ActionContext, MultiStateActionBuilder<T>> {

	private BiConsumer<ActionState<T>, EventTrigger> actionStateChangedCallback;
	private boolean useCheckboxForIcons;

	private List<ActionState<T>> states = new ArrayList<>();
	private Supplier<List<ActionState<T>>> generator = null;

	/**
	 * Builder constructor
	 * 
	 * @param name the name of the action to be built
	 * @param owner the owner of the action to be build
	 */
	public MultiStateActionBuilder(String name, String owner) {
		super(name, owner);
	}

	@Override
	protected MultiStateActionBuilder<T> self() {
		return this;
	}

	/**
	 * Sets the primary callback to be executed when this action changes its action state.
	 * 
	 * <p>
	 * This builder will throw an {@link IllegalStateException} if one of the build methods is
	 * called without providing this callback.
	 * 
	 * @param biConsumer the callback to execute when the selected action state is changed.
	 * @return this builder (for chaining)
	 */
	public MultiStateActionBuilder<T> onActionStateChanged(
			BiConsumer<ActionState<T>, EventTrigger> biConsumer) {

		actionStateChangedCallback = biConsumer;
		return self();
	}

	/**
	 * Overrides the default icons for actions shown in popup menu of the multi-state action.
	 * 
	 * <p>
	 * By default, the popup menu items will use the icons as provided by the {@link ActionState}.
	 * By passing true to this method, icons will not be used in the popup menu. Instead, a checkbox
	 * icon will be used to show the active action state.
	 * 
	 * @param b true to use a checkbox
	 * @return this MultiActionDockingActionBuilder (for chaining)
	 */
	public MultiStateActionBuilder<T> useCheckboxForIcons(boolean b) {
		this.useCheckboxForIcons = b;
		return self();
	}

	/**
	 * Add an action state
	 * 
	 * @param displayName the name to appear in the action menu
	 * @param icon the icon to appear in the action menu
	 * @param userData the data associated with this state
	 * @return this MultiActionDockingActionBuilder (for chaining)
	 */
	public MultiStateActionBuilder<T> addState(String displayName, Icon icon, T userData) {
		states.add(new ActionState<>(displayName, icon, userData));
		return self();
	}

	/**
	 * Add an action state
	 * 
	 * @param actionState the action state to add
	 * @return this MultiActionDockingActionBuilder (for chaining)
	 */
	public MultiStateActionBuilder<T> addState(ActionState<T> actionState) {
		states.add(actionState);
		return self();
	}

	/**
	 * Add a list of action states
	 * 
	 * @param list a list of ActionStates;
	 * @return this MultiActionDockingActionBuilder (for chaining)
	 */
	public MultiStateActionBuilder<T> addStates(List<ActionState<T>> list) {
		states.addAll(list);
		return self();
	}

	/**
	 * Generate the states dynamically upon the user clicking the button
	 * 
	 * <p>
	 * It is highly recommended that the current state is included in the list of available states.
	 * Otherwise, the user could become confused or frustrated.
	 * 
	 * @param generator a function from action context to available states
	 * @return this MultiActionDockingActionBuilder (for chaining)
	 */
	public MultiStateActionBuilder<T> stateGenerator(Supplier<List<ActionState<T>>> generator) {
		this.generator = generator;
		return self();
	}

	@Override
	public MultiStateDockingAction<T> build() {
		validate();
		MultiStateDockingAction<T> action =
			new MultiStateDockingAction<>(name, owner) {

				@Override
				public void actionStateChanged(ActionState<T> newActionState,
						EventTrigger trigger) {
					actionStateChangedCallback.accept(newActionState, trigger);
				}

				@Override
				public void actionPerformed(ActionContext context) {
					if (actionCallback != null) {
						actionCallback.accept(context);
					}
					else {
						super.actionPerformed(context);
					}
				}

				@Override
				protected List<ActionState<T>> getStates() {
					if (generator == null) {
						return super.getStates();
					}
					else {
						return generator.get();
					}
				}
			};

		for (ActionState<T> actionState : states) {
			action.addActionState(actionState);
		}

		decorateAction(action);
		action.setUseCheckboxForIcons(useCheckboxForIcons);
		return action;
	}

	@Override
	protected void validate() {
		if (actionStateChangedCallback == null) {
			throw new IllegalStateException(
				"Can't build a MultiStateDockingAction without an action state changed callback");
		}
	}

}
