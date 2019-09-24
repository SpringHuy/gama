/*********************************************************************************************
 *
 * 'IGamlBuilderListener.java, in plugin gama.core.lang, is part of the source code of the GAMA modeling and
 * simulation platform. (c) 2007-2016 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and developers contact.
 * 
 *
 **********************************************************************************************/
package gama.core.lang.validation;

import gaml.descriptions.IDescription;
import gaml.descriptions.ValidationContext;

/**
 * The class IGamlBuilder.
 * 
 * @author drogoul
 * @since 2 mars 2012
 * 
 */
public interface IGamlBuilderListener {

	void validationEnded(final Iterable<? extends IDescription> experiments, final ValidationContext status);
}