/*******************************************************************************************************
 *
 * msi.gaml.statements.draw.DrawingAttributes.java, in plugin msi.gama.core, is part of the source code of the GAMA
 * modeling and simulation platform (v. 1.8)
 *
 * (c) 2007-2018 UMI 209 UMMISCO IRD/SU & Partners
 *
 * Visit https://github.com/gama-platform/gama for license information and contacts.
 *
 ********************************************************************************************************/
package msi.gaml.statements.draw;

import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import msi.gama.common.geometry.AxisAngle;
import msi.gama.common.geometry.Scaling3D;
import msi.gama.common.preferences.GamaPreferences;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.GamaPoint;
import msi.gama.metamodel.shape.IShape;
import msi.gama.util.GamaColor;
import msi.gama.util.GamaMaterial;
import msi.gama.util.file.GamaGifFile;
import msi.gaml.operators.IUnits;

public class DrawingAttributes {

	private static int INDEX = 0;
	public static final GamaColor TEXTURED_COLOR = new GamaColor(Color.white);
	public static final GamaColor SELECTED_COLOR = new GamaColor(Color.red);

	private final int uniqueIndex;
	GamaPoint location;
	Scaling3D size;
	AxisAngle rotation;
	Double depth = null, lineWidth = GamaPreferences.Displays.CORE_LINE_WIDTH.getValue();
	public IShape.Type type;
	GamaColor fill, highlight, border;
	List<?> textures;
	boolean empty = false, selected = false, synthetic = false, lighting = true;
	GamaMaterial material;

	private DrawingAttributes() {
		uniqueIndex = INDEX++;

	}

	public DrawingAttributes(final Scaling3D size, final AxisAngle rotation, final GamaPoint location,
			final GamaColor color, final GamaColor border, final Boolean lighting) {
		this();
		setBorder(border);
		setFill(color);
		setSize(size);
		setLocation(location == null ? null : new GamaPoint(location));
		setRotation(rotation);
		setLighting(lighting);
	}
	//
	// public void setRotation(final Double angle, final GamaPoint axis) {
	// if (angle == null || axis != null && axis.x == 0d && axis.y == 0d && axis.z == 0d) {
	// setRotation(null);
	// } else if (axis == null) {
	// setRotation(new AxisAngle(Rotation3D.PLUS_K, angle));
	// } else {
	// setRotation(new AxisAngle(axis, angle));
	// }
	// }

	public int getIndex() {
		return uniqueIndex;
	}

	public void setSynthetic(final boolean s) {
		synthetic = s;
	}

	public boolean isSynthetic() {
		return synthetic;
	}

	public void setLighting(final Boolean lighting) {
		if (lighting == null) { return; }
		this.lighting = lighting;
	}

	public void setEmpty(final Boolean b) {
		if (b == null || !b) {
			setFilled();
		} else {
			setEmpty();
		}
	}

	public IAgent getAgentIdentifier() {
		return null;
	}

	public String getSpeciesName() {
		return null;
	}

	/**
	 * Returns the angle of the rotation in degrees (or null if no rotation is defined)
	 *
	 * @return
	 */
	public Double getAngle() {
		if (getRotation() == null) { return null; }
		return getRotation().angle;
	}

	public void setTexture(final Object o) {
		if (o == null) {
			setTextures(null);
		} else {
			setTextures(Arrays.asList(o));
		}
	}

	public void markSelected(final int pickedIndex) {
		setSelected(pickedIndex == uniqueIndex);
	}

	public GamaPoint getAnchor() {
		return IUnits.bottom_left;
	}

	public GamaPoint getLocation() {
		return location;
	}

	public Scaling3D getSize() {
		return size;
	}

	public Double getHeight() {
		return depth;
	}

	public void setLineWidth(final Double d) {
		if (d == null) {
			lineWidth = GamaPreferences.Displays.CORE_LINE_WIDTH.getValue();
		} else {
			lineWidth = d;
		}
	}

	public Double getLineWidth() {
		return lineWidth;
	}

	public IShape.Type getType() {
		return type;
	}

	public boolean useCache() {
		return true;
	}

	public void setType(final IShape.Type type) {
		this.type = type;
	}

	public AxisAngle getRotation() {
		return rotation;
	}

	public void setLocation(final GamaPoint loc) {
		location = loc;
	}

	public void setSize(final Scaling3D size) {
		this.size = size;
	}

	public void setRotation(final AxisAngle rotation) {
		if (rotation == null) { return; }
		this.rotation = rotation;
	}

	public void setHeight(final Double depth) {
		if (depth == null) { return; }
		this.depth = depth;
	}

	public GamaColor getColor() {
		if (selected) { return SELECTED_COLOR; }
		if (highlight != null) { return highlight; }
		if (empty) { return null; }
		if (fill == null) {
			if (textures != null) { return TEXTURED_COLOR; }
			// Always returns the color as we are solid; so null cannot be an option
			// see issue #2724
			return GamaPreferences.Displays.CORE_COLOR.getValue();
			// }
			// return null;
		}
		return fill;
	}

	public GamaColor getBorder() {
		if (empty && border == null) { return fill; }
		return border;
	}

	public void setEmpty() {
		empty = true;
	}

	public void setFilled() {
		empty = false;
	}

	public void setFill(final GamaColor color) {
		fill = color;
	}

	public void setBorder(final GamaColor border) {
		this.border = border;
	}

	void setLighting(final boolean lighting) {
		this.lighting = lighting;
	}

	public void setNoBorder() {
		border = null;
	}

	public void setTextures(final List<?> textures) {
		this.textures = textures;
	}

	public List getTextures() {
		return textures;
	}

	public boolean isEmpty() {
		return empty;
	}

	public boolean isAnimated() {
		if (!useCache()) { return true; }
		if (textures == null) { return false; }
		final Object o = textures.get(0);
		if (!(o instanceof GamaGifFile)) { return false; }
		return true;
	}

	public int getFrameCount() {
		if (textures == null) { return 1; }
		final Object o = textures.get(0);
		if (!(o instanceof GamaGifFile)) { return 1; }
		return ((GamaGifFile) o).getFrameCount();

	}

	public int getAverageDelay() {
		if (textures == null) { return 0; }
		final Object o = textures.get(0);
		if (!(o instanceof GamaGifFile)) { return 0; }
		return ((GamaGifFile) o).getAverageDelay();

	}

	public boolean isLighting() {
		return lighting;
	}

	public void setHighlighted(final GamaColor color) {
		highlight = color;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(final boolean b) {
		selected = b;
	}

	/**
	 * Method getMaterial()
	 *
	 * @see msi.gaml.statements.draw.DrawingAttributes#getMaterial()
	 */
	public GamaMaterial getMaterial() {
		return material;
	}

	public void setMaterial(final GamaMaterial m) {
		material = m;
	}

}