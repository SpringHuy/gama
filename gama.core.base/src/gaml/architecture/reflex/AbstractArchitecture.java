/*******************************************************************************************************
 *
 * gaml.architecture.reflex.AbstractArchitecture.java, in plugin gama.core, is part of the source code of the
 * GAMA modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gaml.architecture.reflex;

import gama.runtime.scope.IScope;
import gaml.architecture.IArchitecture;
import gaml.expressions.IExpression;
import gaml.skills.Skill;
import gaml.species.ISpecies;

public abstract class AbstractArchitecture extends Skill implements IArchitecture {

	@Override
	public <T> T getFacetValue(final IScope scope, final String key, final T defaultValue) {
		return null;
	}

	public AbstractArchitecture() {
		super();
	}

	@Override
	public String serialize(final boolean includingBuiltIn) {
		return getName();
	}

	@Override
	public String getKeyword() {
		return getName();
	}

	@Override
	public String getTrace(final IScope scope) {
		return "";
	}

	@Override
	public IExpression getFacet(final String... key) {
		return null;
	}

	@Override
	public boolean hasFacet(final String key) {
		return false;
	}

	@Override
	public void verifyBehaviors(final ISpecies context) {}

	@Override
	public void dispose() {}

}