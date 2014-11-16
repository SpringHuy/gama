package ummisco.gaml.editbox.impl;

import java.util.*;
import org.eclipse.jface.util.*;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;
import ummisco.gaml.editbox.*;

public class BoxProviderImpl implements IBoxProvider {

	protected String id;
	protected String name;
	protected IBoxSettings editorsSettings;
	protected BoxSettingsStoreImpl settingsStore;
	protected Map<String, Class> builders;
	protected Collection<String> defaultSettingsCatalog;
	private ArrayList<Matcher> matchers;

	@Override
	public BoxSettingsStoreImpl getSettingsStore() {
		if ( settingsStore == null ) {
			settingsStore = createSettingsStore();
			settingsStore.setProviderId(id);
		}
		return settingsStore;
	}

	@Override
	public IBoxSettings getEditorsBoxSettings() {
		if ( editorsSettings == null ) {
			editorsSettings = createSettings0();
			getSettingsStore().loadDefaults(editorsSettings);
			editorsSettings.addPropertyChangeListener(new IPropertyChangeListener() {

				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					String p = event.getProperty();
					if ( p != null &&
						(p.equals(IBoxSettings.PropertiesKeys.FileNames.name()) || p
							.equals(IBoxSettings.PropertiesKeys.ALL.name())) ) {
						matchers = null;
					}
				}
			});
		}
		return editorsSettings;
	}

	@Override
	public IBoxDecorator decorate(final IWorkbenchPart editorPart) {
		if ( !(editorPart instanceof IBoxEnabledEditor) ) { return null; }
		IBoxSettings settings = getEditorsBoxSettings();
		if ( !settings.getEnabled() ) { return null; }
		((IBoxEnabledEditor) editorPart).createDecorator();
		return ((IBoxEnabledEditor) editorPart).getDecorator();
	}

	protected StyledText getStyledText(final IWorkbenchPart editorPart) {
		if ( editorPart != null ) {
			Object obj = editorPart.getAdapter(Control.class);
			if ( obj instanceof StyledText ) { return (StyledText) obj; }
		}

		return null;
	}

	@Override
	public boolean supports(final IWorkbenchPart editorPart) {
		return editorPart.getAdapter(Control.class) instanceof StyledText &&
			(supportsFile(editorPart.getTitle()) || supportsFile(editorPart.getTitleToolTip()));
	}

	protected boolean supportsFile(final String fileName) {
		if ( fileName != null ) {
			for ( Matcher matcher : getMatchers() ) {
				if ( matcher.matches(fileName) ) { return true; }
			}
		}
		return false;
	}

	protected Collection<Matcher> getMatchers() {
		if ( matchers == null ) {
			matchers = new ArrayList<Matcher>();
			Collection<String> fileNames = getEditorsBoxSettings().getFileNames();
			if ( fileNames != null ) {
				for ( String pattern : fileNames ) {
					matchers.add(new Matcher(pattern));
				}
			}
		}
		return matchers;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setId(final String newId) {
		id = newId;
	}

	public void setName(final String newName) {
		name = newName;
	}

	protected BoxSettingsStoreImpl createSettingsStore() {
		BoxSettingsStoreImpl result = new BoxSettingsStoreImpl();
		result.setDefaultSettingsCatalog(defaultSettingsCatalog);
		return result;
	}

	public void setDefaultSettingsCatalog(final Collection<String> cat) {
		defaultSettingsCatalog = cat;
	}

	@Override
	public IBoxSettings createSettings() {
		BoxSettingsImpl result = createSettings0();
		result.copyFrom(getEditorsBoxSettings());
		return result;
	}

	protected BoxSettingsImpl createSettings0() {
		return new BoxSettingsImpl();
	}

	@Override
	public IBoxDecorator createDecorator() {
		BoxDecoratorImpl result = new BoxDecoratorImpl();
		result.setProvider(this);
		return result;
	}

	@Override
	public Collection<String> getBuilders() {
		return builders != null ? builders.keySet() : null;
	}

	public void setBuilders(final Map<String, Class> newBuilders) {
		builders = newBuilders;
	}

	@Override
	public IBoxBuilder createBoxBuilder(final String name) {
		Class c = null;
		if ( name != null && builders != null ) {
			c = builders.get(name);
		}
		if ( c == null ) { return new BoxBuilderImpl(); }
		try {
			return (IBoxBuilder) c.newInstance();
		} catch (Exception e) {
			EditBox.logError(this, "Cannot create box builder: " + name, e);
		}
		return null;
	}

	class Matcher {

		org.eclipse.ui.internal.misc.StringMatcher m;

		Matcher(final String pattern) {
			m = new org.eclipse.ui.internal.misc.StringMatcher(pattern, true, false);
		}

		boolean matches(final String text) {
			return m.match(text);
		}
	}
}
