/*********************************************************************************************
 *
 * 'OpenImportedErrorSelectionListener.java, in plugin gama.ui.base.modeling, is part of the source code of the GAMA
 * modeling and simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.modeling.editor.toolbar;

import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Control;

import gama.GAMA;
import gama.ui.base.menus.GamaMenu;
import gama.ui.base.views.toolbar.Selector;
import gama.ui.modeling.editor.GamlEditor;
import gama.ui.modeling.editor.GamlEditorState;

/**
 * The class CreateExperimentSelectionListener.
 *
 * @author drogoul
 * @since 27 août 2016
 *
 */
public class OpenImportedErrorSelectionListener implements Selector {

	GamlEditor editor;
	GamlEditorState state;

	/**
	 *
	 */
	public OpenImportedErrorSelectionListener(final GamlEditor editor, final GamlEditorState state,
			final Control toolbar) {
		this.editor = editor;
		this.state = state;
	}

	/**
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	@Override
	public void widgetSelected(final SelectionEvent e) {
		final Map<String, URI> msgs = state.getImportedErrors();
		if (!msgs.isEmpty()) {
			final GamaMenu menu = new GamaMenu() {

				@Override
				protected void fillMenu() {

					for (final String s : msgs.keySet()) {
						action(s, new SelectionAdapter() {

							@Override
							public void widgetSelected(final SelectionEvent e1) {
								GAMA.getGui().editModel(msgs.get(s));
							}

						}, null);
					}

				}
			};
			menu.open((Control) e.widget, e, 32);
		}
	}

}
