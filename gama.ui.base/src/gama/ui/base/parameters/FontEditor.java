/*********************************************************************************************
 *
 * 'FontEditor.java, in plugin gama.ui.base.shared, is part of the source code of the GAMA modeling and simulation
 * platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 *
 *
 **********************************************************************************************/
package gama.ui.base.parameters;

import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FontDialog;

import gama.ui.base.controls.FlatButton;
import gama.ui.base.interfaces.EditorListener;
import gama.ui.base.resources.GamaFonts;
import gama.ui.base.resources.IGamaColors;
import gama.ui.base.utils.WorkbenchHelper;
import gama.common.interfaces.IAgent;
import gama.common.interfaces.experiment.IParameter;
import gama.runtime.scope.IScope;
import gama.util.GamaFont;

public class FontEditor extends AbstractEditor<GamaFont> {

	private FlatButton edit;

	FontEditor(final IScope scope, final IAgent agent, final IParameter param, final EditorListener<GamaFont> l) {
		super(scope, agent, param, l);
	}

	@Override
	public Control createCustomParameterControl(final Composite compo) {
		edit = FlatButton.menu(compo, IGamaColors.GRAY_LABEL, "").light().small();
		edit.addSelectionListener(this);
		displayParameterValue();
		return edit;
	}

	@Override
	protected void displayParameterValue() {
		internalModification = true;
		final GamaFont data =
				currentValue != null ? currentValue : toGamaFont(GamaFonts.getSmallFont().getFontData()[0]);
		edit.setText(data.toString());
		edit.setFont(new Font(WorkbenchHelper.getDisplay(), toFontData(data)));
		internalModification = false;
	}

	private GamaFont toGamaFont(final FontData fd) {
		return new GamaFont(fd.getName(), fd.getStyle(), fd.getHeight());
	}

	private FontData toFontData(final GamaFont gf) {
		return new FontData(gf.getName(), gf.getSize(), gf.getStyle());
	}

	@Override
	public Control getEditorControl() {
		return edit;
	}

	@Override
	protected int[] getToolItems() {
		return new int[] { EDIT, REVERT };
	}

	@Override
	public void widgetSelected(final SelectionEvent e) {
		final FontDialog dialog = new FontDialog(WorkbenchHelper.getShell());
		dialog.setEffectsVisible(false);
		FontData data = toFontData(currentValue);
		dialog.setFontList(new FontData[] { data });
		data = dialog.open();
		if (data != null) {
			modifyAndDisplayValue(toGamaFont(data));
		}

	}
}