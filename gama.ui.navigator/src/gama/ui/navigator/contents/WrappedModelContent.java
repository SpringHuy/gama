package gama.ui.navigator.contents;

import gaml.compilation.interfaces.ISyntacticElement;

public class WrappedModelContent extends WrappedSyntacticContent {

	public WrappedModelContent(final WrappedGamaFile file, final ISyntacticElement e) {
		super(file, e, "Contents");
	}

	@Override
	public WrappedGamaFile getFile() {
		return (WrappedGamaFile) getParent();
	}

	@Override
	public boolean hasChildren() {
		return true;
	}

}