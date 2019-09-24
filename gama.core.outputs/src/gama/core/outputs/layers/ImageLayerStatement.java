/*******************************************************************************************************
 *
 * gama.core.outputs.layers.ImageLayerStatement.java, in plugin msi.gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package gama.core.outputs.layers;

import gama.core.outputs.layers.ImageLayerStatement.ImageLayerValidator;
import gama.processor.annotations.IConcept;
import gama.processor.annotations.ISymbolKind;
import gama.processor.annotations.GamlAnnotations.doc;
import gama.processor.annotations.GamlAnnotations.example;
import gama.processor.annotations.GamlAnnotations.facet;
import gama.processor.annotations.GamlAnnotations.facets;
import gama.processor.annotations.GamlAnnotations.inside;
import gama.processor.annotations.GamlAnnotations.symbol;
import gama.processor.annotations.GamlAnnotations.usage;
import gama.common.interfaces.IGamlIssue;
import gama.common.interfaces.IKeyword;
import gama.runtime.exceptions.GamaRuntimeException;
import gama.runtime.scope.IScope;
import gaml.compilation.annotations.validator;
import gaml.compilation.interfaces.IDescriptionValidator;
import gaml.descriptions.IDescription;
import gaml.descriptions.StatementDescription;
import gaml.expressions.IExpression;
import gaml.expressions.IExpressionFactory;
import gaml.operators.Cast;
import gaml.types.IType;

/**
 * Written by drogoul Modified on 9 nov. 2009
 *
 * @todo Description
 *
 */
@symbol (
		name = IKeyword.IMAGE,
		kind = ISymbolKind.LAYER,
		with_sequence = false,
		concept = { IConcept.DISPLAY, IConcept.FILE, IConcept.LOAD_FILE })
@inside (
		symbols = IKeyword.DISPLAY)
@facets (
		value = { @facet (
				name = IKeyword.FILE,
				type = { IType.STRING, IType.FILE },
				optional = true,
				doc = @doc ("the name/path of the image (in the case of a raster image)")),
				@facet (
						name = IKeyword.POSITION,
						type = IType.POINT,
						optional = true,
						doc = @doc ("position of the upper-left corner of the layer. Note that if coordinates are in [0,1[, the position is relative to the size of the environment (e.g. {0.5,0.5} refers to the middle of the display) whereas it is absolute when coordinates are greater than 1 for x and y. The z-ordinate can only be defined between 0 and 1. The position can only be a 3D point {0.5, 0.5, 0.5}, the last coordinate specifying the elevation of the layer.")),
				@facet (
						name = IKeyword.SIZE,
						type = IType.POINT,
						optional = true,
						doc = @doc ("extent of the layer in the screen from its position. Coordinates in [0,1[ are treated as percentages of the total surface, while coordinates > 1 are treated as absolute sizes in model units (i.e. considering the model occupies the entire view). Like in 'position', an elevation can be provided with the z coordinate, allowing to scale the layer in the 3 directions ")),
				@facet (
						name = IKeyword.TRANSPARENCY,
						type = IType.FLOAT,
						optional = true,
						doc = @doc ("the transparency rate of the agents (between 0 and 1, 1 means no transparency)")),
				@facet (
						name = IKeyword.NAME,
						type = { IType.STRING, IType.FILE },
						optional = true,
						doc = @doc ("Human readable title of the image layer")),
				@facet (
						name = IKeyword.GIS,
						type = { IType.FILE, IType.STRING },
						optional = true,
						doc = @doc ("the name/path of the shape file (to display a shapefile as background, without creating agents from it)")),
				@facet (
						name = IKeyword.COLOR,
						type = IType.COLOR,
						optional = true,
						doc = @doc ("in the case of a shapefile, this the color used to fill in geometries of the shapefile. In the case of an image, it is used to tint the image")),
				@facet (
						name = IKeyword.REFRESH,
						type = IType.BOOL,
						optional = true,
						doc = @doc ("(openGL only) specify whether the image display is refreshed or not. (false by default, true should be used in cases of images that are modified over the simulation)")) },
		omissible = IKeyword.NAME)
@doc (
		value = "`" + IKeyword.IMAGE
				+ "` allows modeler to display an image (e.g. as background of a simulation). Note that this image will not be dynamically changed or moved in OpenGL, unless the refresh: facet is set to true.",
		usages = { @usage (
				value = "The general syntax is:",
				examples = { @example (
						value = "display my_display {",
						isExecutable = false),
						@example (
								value = "   image layer_name file: image_file [additional options];",
								isExecutable = false),
						@example (
								value = "}",
								isExecutable = false) }),
				@usage (
						value = "For instance, in the case of a bitmap image",
						examples = { @example (
								value = "display my_display {",
								isExecutable = false),
								@example (
										value = "   image background file:\"../images/my_backgound.jpg\";",
										isExecutable = false),
								@example (
										value = "}",
										isExecutable = false) }),
				@usage (
						value = "Or in the case of a shapefile:",
						examples = { @example (
								value = "display my_display {",
								isExecutable = false),
								@example (
										value = "   image testGIS gis: \"../includes/building.shp\" color: rgb('blue');",
										isExecutable = false),
								@example (
										value = "}",
										isExecutable = false) }),
				@usage (
						value = "It is also possible to superpose images on different layers in the same way as for species using opengl display:",
						examples = { @example (
								value = "display my_display {",
								isExecutable = false),
								@example (
										value = "  image image1 file:\"../images/image1.jpg\";",
										isExecutable = false),
								@example (
										value = "  image image2 file:\"../images/image2.jpg\";",
										isExecutable = false),
								@example (
										value = "  image image3 file:\"../images/image3.jpg\" position: {0,0,0.5};",
										isExecutable = false),
								@example (
										value = "}",
										isExecutable = false) }) },
		see = { IKeyword.DISPLAY, IKeyword.AGENTS, IKeyword.CHART, IKeyword.EVENT, "graphics", IKeyword.GRID_POPULATION,
				IKeyword.OVERLAY, IKeyword.POPULATION })
@validator (ImageLayerValidator.class)
public class ImageLayerStatement extends AbstractLayerStatement {

	public static class ImageLayerValidator implements IDescriptionValidator<StatementDescription> {

		@Override
		public void validate(final StatementDescription description) {
			if (!description.hasFacet(GIS)) {
				if (!description.hasFacet(NAME) && !description.hasFacet(FILE)) {
					description.error("Missing facets " + IKeyword.NAME + " or " + IKeyword.FILE,
							IGamlIssue.MISSING_FACET, description.getUnderlyingElement(), FILE, "\"\"");
				}
			} else {
				if (description.hasFacet(FILE)) {
					description.error("gis: and file: cannot be defined at the same time",
							IGamlIssue.CONFLICTING_FACETS);
				}
			}
		}

	}

	IExpression file;

	public ImageLayerStatement(final IDescription desc) throws GamaRuntimeException {
		super(desc);
		file = getFacet(IKeyword.FILE, IKeyword.NAME);
	}

	/**
	 * In this particular case, returns false by default;
	 */
	@Override
	public IExpression getRefreshFacet() {
		IExpression exp = super.getRefreshFacet();
		if (exp == null) {
			exp = IExpressionFactory.FALSE_EXPR;
		}
		return exp;
	}

	@Override
	public LayerType getType(final boolean output) {
		if (hasFacet(IKeyword.GIS))
			return LayerType.GIS;
		return LayerType.IMAGE;
	}

	// FIXME Use GamaImageFile
	@Override
	public boolean _init(final IScope scope) throws GamaRuntimeException {
		if (file.isConst()) {
			setName(Cast.asString(scope, file.value(scope)));
		} else {
			setName(file.serialize(false));
		}
		return true;
	}

	@Override
	public boolean _step(final IScope scope) throws GamaRuntimeException {
		return true;
	}

}