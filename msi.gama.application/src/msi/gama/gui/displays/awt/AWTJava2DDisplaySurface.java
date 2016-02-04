/*********************************************************************************************
 *
 *
 * 'AbstractAWTDisplaySurface.java', in plugin 'msi.gama.application', is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 *
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 *
 *
 **********************************************************************************************/
package msi.gama.gui.displays.awt;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import com.vividsolutions.jts.geom.Envelope;
import msi.gama.common.interfaces.*;
import msi.gama.common.util.*;
import msi.gama.gui.swt.swing.OutputSynchronizer;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.metamodel.shape.*;
import msi.gama.outputs.*;
import msi.gama.outputs.LayeredDisplayData.Changes;
import msi.gama.outputs.display.*;
import msi.gama.outputs.layers.IEventLayerListener;
import msi.gama.precompiler.GamlAnnotations.display;
import msi.gama.runtime.*;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.expressions.IExpression;
import msi.gaml.operators.*;

@display("java2D")
public class AWTJava2DDisplaySurface extends JPanel implements IDisplaySurface {

	private final LayeredDisplayOutput output;
	// protected final LayeredDisplayData data;
	protected final Rectangle viewPort = new Rectangle();
	protected final ILayerManager manager;
	protected final AffineTransform translation = new AffineTransform();

	protected IGraphics iGraphics;

	protected DisplaySurfaceMenu menuManager;
	protected volatile boolean canBeUpdated = true;
	private volatile boolean lockAcquired = false;
	protected IExpression temp_focus;

	protected Dimension previousPanelSize;
	protected double zoomIncrement = 0.1;
	protected boolean zoomFit = true;
	protected boolean disposed;
	// private IZoomListener zoomListener;

	private IScope scope;
	private Point mousePosition;
	private BufferedImage buffImage;
	private boolean alreadyZooming = false;

	private class DisplayMouseListener extends MouseAdapter {

		boolean dragging;

		@Override
		public void mouseDragged(final MouseEvent e) {
			if ( SwingUtilities.isLeftMouseButton(e) ) {
				dragging = true;
				canBeUpdated = false;
				final Point p = e.getPoint();
				if ( mousePosition == null ) {
					mousePosition = new Point(getWidth() / 2, getHeight() / 2);
				}
				Point origin = getOrigin();
				setOrigin(origin.x + p.x - mousePosition.x, origin.y + p.y - mousePosition.y);
				mousePosition = p;
				repaint();
			}
		}

		@Override
		public void mouseMoved(final MouseEvent e) {
			// we need the mouse position so that after zooming
			// that position of the image is maintained
			mousePosition = e.getPoint();
		}

		@Override
		public void mouseWheelMoved(final MouseWheelEvent e) {
			final boolean zoomIn = e.getWheelRotation() < 0;
			mousePosition = e.getPoint();
			Point p = new Point(mousePosition.x, mousePosition.y);
			double zoomFactor = applyZoom(zoomIn ? 1.0 + zoomIncrement : 1.0 - zoomIncrement);
			Point origin = getOrigin();
			double newx = Math.round(zoomFactor * (p.x - origin.x) - p.x + getWidth() / 2d);
			double newy = Math.round(zoomFactor * (p.y - origin.y) - p.y + getHeight() / 2d);
			centerOnDisplayCoordinates(new Point((int) newx, (int) newy));
			updateDisplay(true);
		}

		@Override
		public void mouseClicked(final MouseEvent evt) {
			if ( evt.getClickCount() == 2 ) {
				zoomFit();
			} else if ( evt.isControlDown() || evt.isMetaDown() || evt.isPopupTrigger() ) {
				selectAgents(evt.getX(), evt.getY());
			}
		}

		@Override
		public void mouseReleased(final MouseEvent e) {
			if ( dragging ) {
				canBeUpdated = true;
				updateDisplay(true);
				dragging = false;

			}

		}

	}

	public AWTJava2DDisplaySurface(final Object ... args) {
		output = (LayeredDisplayOutput) args[0];
		output.setSurface(this);
		setDisplayScope(output.getScope().copy());
		// data = output.getData();
		output.getData().addListener(this);
		temp_focus = output.getFacet(IKeyword.FOCUS);
		setOpaque(true);
		setDoubleBuffered(true);
		// Experimental
		// setIgnoreRepaint(true);

		//

		setLayout(new BorderLayout());
		setBackground(output.getData().getBackgroundColor());
		setName(output.getName());
		manager = new LayerManager(this, output);
		final DisplayMouseListener d = new DisplayMouseListener();
		addMouseListener(d);
		addMouseMotionListener(d);
		addMouseWheelListener(d);
		addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(final ComponentEvent e) {
				if ( buffImage == null || zoomFit ) {
					zoomFit();
				} else {
					if ( isFullImageInPanel() ) {
						centerImage();
					} else if ( isImageEdgeInPanel() ) {
						scaleOrigin();
					}
					updateDisplay(true);
				}
				final double newZoom = Math.min(getWidth() / getDisplayWidth(), getHeight() / getDisplayHeight());
				newZoomLevel(1 / newZoom);
				previousPanelSize = getSize();
			}
		});

	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}

	@Override
	public void outputReloaded() {
		// We first copy the scope
		setDisplayScope(output.getScope().copy());
		// We disable error reporting
		getDisplayScope().disableErrorReporting();
		if ( iGraphics != null ) {
			iGraphics.initFor(this);
		}
		manager.outputChanged();

		resizeImage(getWidth(), getHeight(), true);
		if ( zoomFit ) {
			zoomFit();
		}
		repaint();
	}

	@Override
	public IScope getDisplayScope() {
		return scope;
	}

	// FIXME Ugly code. The hack must be better written
	@Override
	public void setSWTMenuManager(final Object manager) {
		menuManager = (DisplaySurfaceMenu) manager;
	}

	@Override
	public ILayerManager getManager() {
		return manager;
	}

	public Point getOrigin() {
		return viewPort.getLocation();
	}

	@Override
	public void setFont(final Font f) {
		// super.setFont(null);
	}

	/**
	 * @see msi.gama.common.interfaces.IDisplaySurface#snapshot()
	 */
	@Override
	public void snapshot() {
		// TODO DEBUG
		this.getVisibleRegionForLayer(manager.getItems().get(0));
		LayeredDisplayData data = output.getData();
		if ( data.getImageDimension().getX() == -1 && data.getImageDimension().getY() == -1 ) {
			save(getDisplayScope(), getImage());
			return;
		}

		final BufferedImage newImage =
			ImageUtils.createCompatibleImage(data.getImageDimension().getX(), data.getImageDimension().getY());
		final IGraphics tempGraphics = new AWTDisplayGraphics(this, (Graphics2D) newImage.getGraphics());
		tempGraphics.fillBackground(getBackground(), 1);
		manager.drawLayersOn(tempGraphics);
		save(getDisplayScope(), newImage);
		newImage.flush();

	}

	/**
	 * Save this surface into an image passed as a parameter
	 * @param scope
	 * @param image
	 */
	public final void save(final IScope scope, final RenderedImage image) {
		// Intentionnaly passing GAMA.getRuntimeScope() to errors in order to prevent the exceptions from being masked.
		if ( image == null ) { return; }
		try {
			Files.newFolder(scope, SNAPSHOT_FOLDER_NAME);
		} catch (GamaRuntimeException e1) {
			e1.addContext("Impossible to create folder " + SNAPSHOT_FOLDER_NAME);
			GAMA.reportError(GAMA.getRuntimeScope(), e1, false);
			e1.printStackTrace();
			return;
		}
		String snapshotFile = FileUtils.constructAbsoluteFilePath(scope,
			SNAPSHOT_FOLDER_NAME + "/" + GAMA.getModel().getName() + "_display_" + output.getName(), false);

		String file = snapshotFile + "_size_" + image.getWidth() + "x" + image.getHeight() + "_cycle_" +
			scope.getClock().getCycle() + "_time_" + java.lang.System.currentTimeMillis() + ".png";
		DataOutputStream os = null;
		try {
			os = new DataOutputStream(new FileOutputStream(file));
			ImageIO.write(image, "png", os);
		} catch (java.io.IOException ex) {
			GamaRuntimeException e = GamaRuntimeException.create(ex, scope);
			e.addContext("Unable to create output stream for snapshot image");
			GAMA.reportError(GAMA.getRuntimeScope(), e, false);
		} finally {
			try {
				if ( os != null ) {
					os.close();
				}
			} catch (Exception ex) {
				GamaRuntimeException e = GamaRuntimeException.create(ex, scope);
				e.addContext("Unable to close output stream for snapshot image");
				GAMA.reportError(GAMA.getRuntimeScope(), e, false);
			}
		}
	}
	//
	// @Override
	// public InputContext getInputContext() {
	// return null;
	// }

	@Override
	public void removeNotify() {
		// dispose();
		super.removeNotify();
		OutputSynchronizer.decClosingViews(getOutput().getName());
	}

	protected void scaleOrigin() {
		Point origin = getOrigin();
		setOrigin(origin.x * getWidth() / previousPanelSize.width, origin.y * getHeight() / previousPanelSize.height);
		repaint();
	}

	protected void centerImage() {
		setOrigin((int) Math.round((getWidth() - getDisplayWidth()) / 2),
			(int) Math.round((getHeight() - getDisplayHeight()) / 2));
	}

	protected int getOriginX() {
		return getOrigin().x;
	}

	protected int getOriginY() {
		return getOrigin().y;
	}

	@Override
	public BufferedImage getImage() {

		if ( buffImage == null ) {

			BufferedImage image = new BufferedImage(getSize().width, getSize().height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = image.createGraphics();

			// Paint a background for non-opaque components,
			// otherwise the background will be black

			if ( !isOpaque() ) {
				g2d.setColor(getBackground());
				g2d.fillRect(0, 0, getSize().width, getSize().height);
			}

			paint(g2d);
			g2d.dispose();
			return image;
		} else {
			while (!canBeUpdated) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			try {
				canBeUpdated = false;

				return ImageUtils.toCompatibleImage(buffImage);
			} finally {
				canBeUpdated = true;
			}
		}

		// Robot screenRobot;
		// try {
		// screenRobot = new Robot();
		// } catch (AWTException e) {
		// e.printStackTrace();
		// return null;
		// }
		// if ( isDisplayable() && isShowing() ) {
		// Rectangle rectangle = new Rectangle(this.getLocationOnScreen(), this.getSize());
		// final BufferedImage buffImage = screenRobot.createScreenCapture(rectangle);
		// return buffImage;
		// }
		// return null;
		// = renderer.getScreenShot();

	}

	// @Override
	// public void canBeUpdated(final boolean canBeUpdated) {
	// this.canBeUpdated = canBeUpdated;
	// }
	//
	// public boolean canBeUpdated() {
	// return canBeUpdated && iGraphics != null;
	// }

	protected void setOrigin(final int x, final int y) {
		viewPort.x = x;
		viewPort.y = y;
		translation.setToTranslation(x, y);
	}

	private final Runnable displayRunnable = new Runnable() {

		@Override
		public void run() {
			if ( disposed ) { return; }
			if ( iGraphics == null ) { return; }
			try {
				canBeUpdated = false;
				iGraphics.fillBackground(getBackground(), 1);
				manager.drawLayersOn(iGraphics);
				repaint();
			} finally {
				canBeUpdated = true;
			}
		}
	};

	@Override
	public void updateDisplay(final boolean force) {
		if ( disposed ) { return; }
		if ( !canBeUpdated ) { return; }
		if ( /* GAMA.isPaused() || */EventQueue.isDispatchThread() ) {
			EventQueue.invokeLater(displayRunnable);
		} else {
			try {
				EventQueue.invokeAndWait(displayRunnable);
			} catch (InterruptedException e) {
				// e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}

		if ( temp_focus != null ) {
			IShape geometry = Cast.asGeometry(getDisplayScope(), temp_focus.value(getDisplayScope()), false);
			if ( geometry != null ) {
				Rectangle2D r = this.getManager().focusOn(geometry, this);
				java.lang.System.out.println("Rectangle = " + r);
				if ( r == null ) { return; }
				double xScale = getWidth() / r.getWidth();
				double yScale = getHeight() / r.getHeight();
				double zoomFactor = Math.min(xScale, yScale);
				Point center = new Point((int) Math.round(r.getCenterX()), (int) Math.round(r.getCenterY()));

				zoomFactor = applyZoom(zoomFactor);
				center.setLocation(center.x * zoomFactor, center.y * zoomFactor);
				centerOnDisplayCoordinates(center);
			}
			temp_focus = null;
			// Recursive call
			updateDisplay(true);
		}

		// else {
		// EventQueue.invokeLater(displayRunnable);
		// }

		// EXPERIMENTAL
	}

	@Override
	public void zoomIn() {
		if ( alreadyZooming ) { return; }
		alreadyZooming = true;
		Point origin = getOrigin();
		mousePosition = new Point(getWidth() / 2, getHeight() / 2);
		double zoomFactor = applyZoom(1.0 + zoomIncrement);
		double newx = Math.round(zoomFactor * (getWidth() / 2 - origin.x));
		double newy = Math.round(zoomFactor * (getHeight() / 2 - origin.y));
		centerOnDisplayCoordinates(new Point((int) newx, (int) newy));
		updateDisplay(true);
		alreadyZooming = false;
	}

	@Override
	public void zoomOut() {
		if ( alreadyZooming ) { return; }
		alreadyZooming = true;
		Point origin = getOrigin();
		mousePosition = new Point(getWidth() / 2, getHeight() / 2);
		double zoomFactor = applyZoom(1.0 - zoomIncrement);
		double newx = Math.round(zoomFactor * (getWidth() / 2 - origin.x));
		double newy = Math.round(zoomFactor * (getHeight() / 2 - origin.y));
		centerOnDisplayCoordinates(new Point((int) newx, (int) newy));
		updateDisplay(true);
		alreadyZooming = false;
	}

	// Used when the image is resized.
	public boolean isImageEdgeInPanel() {
		if ( previousPanelSize == null ) { return false; }
		Point origin = getOrigin();
		return origin.x > 0 && origin.x < previousPanelSize.width ||
			origin.y > 0 && origin.y < previousPanelSize.height;
	}

	// Tests whether the image is displayed in its entirety in the panel.
	public boolean isFullImageInPanel() {
		Point origin = getOrigin();
		return origin.x >= 0 && origin.x + getDisplayWidth() < getWidth() && origin.y >= 0 &&
			origin.y + getDisplayHeight() < getHeight();
	}

	@Override
	public boolean resizeImage(final int x, final int y, final boolean force) {
		if ( !force && x == viewPort.width && y == viewPort.height ) { return true; }
		if ( getWidth() <= 0 && getHeight() <= 0 ) { return false; }
		try {
			canBeUpdated = false;
			int[] point = computeBoundsFrom(x, y);
			int imageWidth = Math.max(1, point[0]);
			int imageHeight = Math.max(1, point[1]);
			final BufferedImage newImage = ImageUtils.createCompatibleImage(imageWidth, imageHeight);
			if ( buffImage != null ) {
				newImage.getGraphics().drawImage(buffImage, 0, 0, imageWidth, imageHeight, null);
				buffImage.flush();
			}
			buffImage = newImage;
			setDisplayHeight(imageHeight);
			setDisplayWidth(imageWidth);
			iGraphics = new AWTDisplayGraphics(this, buffImage.createGraphics());
		} finally {
			canBeUpdated = true;
		}
		return true;

	}
	//
	// @Override
	// public void paint(final Graphics g) {
	// super.paint(g);
	// ((Graphics2D) g).drawImage(buffImage, translation, null);
	// if ( data.isAutosave() ) {
	// snapshot();
	// }
	// }

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		((Graphics2D) g).drawImage(buffImage, translation, null);
		if ( output.getData().isAutosave() ) {
			snapshot();
		}
	}

	@Override
	public ILocation getModelCoordinates() {
		Point origin = getOrigin();
		if ( mousePosition == null ) { return null; }
		final int xc = mousePosition.x - origin.x;
		final int yc = mousePosition.y - origin.y;
		List<ILayer> layers = manager.getLayersIntersecting(xc, yc);
		if ( layers.isEmpty() ) { return null; }
		return layers.get(0).getModelCoordinatesFrom(xc, yc, this);
	}

	@Override
	public double getEnvWidth() {
		return output.getData().getEnvWidth();
	}

	@Override
	public double getEnvHeight() {
		return output.getData().getEnvHeight();
	}

	@Override
	public double getDisplayWidth() {
		return viewPort.width;
	}

	protected void setDisplayWidth(final int displayWidth) {
		viewPort.width = displayWidth;
	}

	@Override
	public LayeredDisplayData getData() {
		return output.getData();
	}

	@Override
	public double getDisplayHeight() {
		return viewPort.height;
	}

	protected void setDisplayHeight(final int displayHeight) {
		viewPort.height = displayHeight;
	}

	@Override
	public LayeredDisplayOutput getOutput() {
		return output;
	}

	public void newZoomLevel(final double newZoomLevel) {
		getData().setZoomLevel(newZoomLevel);
		// if ( zoomListener != null ) {
		// zoomListener.newZoomLevel(getData().getZoomLevel());
		// }
	}

	@Override
	public double getZoomLevel() {
		if ( getData().getZoomLevel() == null ) {
			getData().setZoomLevel(computeInitialZoomLevel());
		}
		return getData().getZoomLevel();
	}
	//
	// @Override
	// public void setZoomListener(final IZoomListener listener) {
	// zoomListener = listener;
	// }

	@Override
	public void zoomFit() {
		mousePosition = new Point(getWidth() / 2, getHeight() / 2);
		if ( resizeImage(getWidth(), getHeight(), false) ) {
			newZoomLevel(1d);
			zoomFit = true;
			centerImage();
			updateDisplay(true);
		}
	}

	private int[] computeBoundsFrom(final int vwidth, final int vheight) {
		if ( !manager.stayProportional() ) { return new int[] { vwidth, vheight }; }
		final int[] dim = new int[2];
		double widthHeightConstraint = getEnvHeight() / getEnvWidth();
		if ( widthHeightConstraint < 1 ) {
			dim[1] = Math.min(vheight, (int) Math.round(vwidth * widthHeightConstraint));
			dim[0] = Math.min(vwidth, (int) Math.round(dim[1] / widthHeightConstraint));
		} else {
			dim[0] = Math.min(vwidth, (int) Math.round(vheight / widthHeightConstraint));
			dim[1] = Math.min(vheight, (int) Math.round(dim[0] * widthHeightConstraint));
		}
		return dim;
	}

	@Override
	public GamaPoint getModelCoordinatesFrom(final int xOnScreen, final int yOnScreen, final Point sizeInPixels,
		final Point positionInPixels) {
		final double xScale = sizeInPixels.x / getEnvWidth();
		final double yScale = sizeInPixels.y / getEnvHeight();
		final int xInDisplay = xOnScreen - positionInPixels.x;
		final int yInDisplay = yOnScreen - positionInPixels.y;
		final double xInModel = xInDisplay / xScale;
		final double yInModel = yInDisplay / yScale;
		return new GamaPoint(xInModel, yInModel);
	}

	@Override
	public Envelope getVisibleRegionForLayer(final ILayer currentLayer) {
		Envelope e = new Envelope();
		Point origin = getOrigin();
		int xc = -origin.x;
		int yc = -origin.y;
		e.expandToInclude((GamaPoint) currentLayer.getModelCoordinatesFrom(xc, yc, this));
		xc = xc + this.getWidth();
		yc = yc + this.getHeight();
		e.expandToInclude((GamaPoint) currentLayer.getModelCoordinatesFrom(xc, yc, this));
		return e;
	}

	@Override
	public Collection<IAgent> selectAgent(final int x, final int y) {
		int xc = x - getOriginX();
		int yc = y - getOriginY();
		List<IAgent> result = new ArrayList();
		final List<ILayer> layers = getManager().getLayersIntersecting(xc, yc);
		for ( ILayer layer : layers ) {
			Set<IAgent> agents = layer.collectAgentsAt(xc, yc, this);
			if ( !agents.isEmpty() ) {
				result.addAll(agents);
			}
		}
		return result;
	}

	protected void setDisplayScope(final IScope scope) {
		if ( this.scope != null ) {
			GAMA.releaseScope(this.scope);
		}
		this.scope = scope;
	}

	@Override
	public void runAndUpdate(final Runnable r) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				while (!canBeUpdated) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				r.run();
				if ( output.isPaused() || GAMA.isPaused() ) {
					updateDisplay(true);
				}
			}
		}).start();
	}

	@Override
	public void dispose() {
		java.lang.System.out.println("Disposing Java2D display");
		getData().removeListener(this);
		if ( disposed ) { return; }
		disposed = true;
		if ( manager != null ) {
			manager.dispose();
		}
		if ( buffImage != null ) {
			buffImage.flush();
		}
		GAMA.releaseScope(getDisplayScope());
		setDisplayScope(null);
	}

	Map<IEventLayerListener, MouseAdapter> listeners = new HashMap();

	@Override
	public void addListener(final IEventLayerListener listener) {
		if ( listeners.containsKey(listener) ) { return; }

		MouseAdapter l = new MouseAdapter() {

			@Override
			public void mouseClicked(final MouseEvent e) {
				listener.mouseClicked(e.getX(), e.getY(), e.getButton());
			}

			@Override
			public void mousePressed(final MouseEvent e) {
				listener.mouseDown(e.getX(), e.getY(), e.getButton());
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				listener.mouseUp(e.getX(), e.getY(), e.getButton());
			}

			@Override
			public void mouseMoved(final MouseEvent e) {
				if ( e.getButton() > 0 ) { return; }
				listener.mouseMove(e.getX(), e.getY());
			}

			@Override
			public void mouseEntered(final MouseEvent e) {
				if ( e.getButton() > 0 ) { return; }
				listener.mouseEnter(e.getX(), e.getY());
			}

			@Override
			public void mouseExited(final MouseEvent e) {
				if ( e.getButton() > 0 ) { return; }
				listener.mouseExit(e.getX(), e.getY());
			}

		};
		listeners.put(listener, l);
		addMouseListener(l);
		addMouseMotionListener(l);
	}

	@Override
	public void removeListener(final IEventLayerListener listener) {
		MouseAdapter l = listeners.get(listener);
		if ( l == null ) { return; }
		listeners.remove(listener);
		super.removeMouseListener(l);
		super.removeMouseMotionListener(l);
	}

	@Override
	public Collection<IEventLayerListener> getLayerListeners() {
		return listeners.keySet();
	}

	/**
	 * Method followAgent()
	 * @see msi.gama.common.interfaces.IDisplaySurface#followAgent(msi.gama.metamodel.agent.IAgent)
	 */
	@Override
	public void followAgent(final IAgent a) {}

	/**
	 * Method computeInitialZoomLevel()
	 * @see msi.gama.gui.displays.awt.AbstractAWTDisplaySurface#computeInitialZoomLevel()
	 */
	protected Double computeInitialZoomLevel() {
		return 1.0;
	}

	@Override
	public void setBounds(final int arg0, final int arg1, final int arg2, final int arg3) {
		// scope.getGui().debug("Set bounds called with " + arg2 + " " + arg3);
		if ( arg2 == 0 && arg3 == 0 ) { return; }
		super.setBounds(arg0, arg1, arg2, arg3);
	}

	@Override
	public void setBounds(final Rectangle r) {
		// scope.getGui().debug("Set bounds called with " + r);
		if ( r.width < 1 && r.height < 1 ) { return; }
		super.setBounds(r);
	}

	public double applyZoom(final double factor) {
		double real_factor = Math.min(factor, 10 / getZoomLevel());
		boolean success = false;

		try {
			success = resizeImage(Math.max(1, (int) Math.round(getDisplayWidth() * real_factor)),
				Math.max(1, (int) Math.round(getDisplayHeight() * real_factor)), false);
		} catch (Exception e) {
			// System.gc();
			// scope.getGui().debug("AWTDisplaySurface.applyZoom: not enough memory available to zoom at :" + real_factor);
			real_factor = MAX_ZOOM_FACTOR;
			try {
				success = resizeImage(Math.max(1, (int) Math.round(getDisplayWidth() * real_factor)),
					Math.max(1, (int) Math.round(getDisplayHeight() * real_factor)), false);
			} catch (Exception e1) {
				// scope.getGui().debug("AWTDisplaySurface.applyZoom : not enough memory available to zoom at :" +
				// real_factor);
				real_factor = 1;
				success = true;
			} catch (Error e1) {
				// scope.getGui().debug("AWTDisplaySurface.applyZoom : not enough memory available to zoom at :" +
				// real_factor);
				real_factor = 1;
				success = true;
			}
		} catch (Error e) {
			java.lang.System.gc();
			// scope.getGui().debug("AWTDisplaySurface.applyZoom: not enough memory available to zoom at :" + real_factor);
			real_factor = MAX_ZOOM_FACTOR;
			try {
				success = resizeImage(Math.max(1, (int) Math.round(getDisplayWidth() * real_factor)),
					Math.max(1, (int) Math.round(getDisplayHeight() * real_factor)), false);
			} catch (Exception e1) {
				// scope.getGui().debug("AWTDisplaySurface.applyZoom : not enough memory available to zoom at :" +
				// real_factor);
				real_factor = 1;
				success = true;
			} catch (Error e1) {
				// scope.getGui().debug("AWTDisplaySurface.applyZoom : not enough memory available to zoom at :" +
				// real_factor);
				real_factor = 1;
				success = true;
			}
		}

		if ( success ) {
			zoomFit = false;
			double widthHeightConstraint = getEnvHeight() / getEnvWidth();

			if ( widthHeightConstraint < 1 ) {
				newZoomLevel(getDisplayWidth() / getWidth());
			} else {
				newZoomLevel(getDisplayHeight() / getHeight());
			}
		}
		return real_factor;
	}

	@Override
	public void focusOn(final IShape geometry) {
		Rectangle2D r = this.getManager().focusOn(geometry, this);
		if ( r == null ) { return; }
		double xScale = getWidth() / r.getWidth();
		double yScale = getHeight() / r.getHeight();
		double zoomFactor = Math.min(xScale, yScale);
		Point center = new Point((int) Math.round(r.getCenterX()), (int) Math.round(r.getCenterY()));

		zoomFactor = applyZoom(zoomFactor);
		center.setLocation(center.x * zoomFactor, center.y * zoomFactor);
		centerOnDisplayCoordinates(center);

		updateDisplay(true);
	}

	public void centerOnViewCoordinates(final Point p) {
		Point origin = getOrigin();
		int translationX = p.x - Math.round(getWidth() / (float) 2);
		int translationY = p.y - Math.round(getHeight() / (float) 2);
		setOrigin(origin.x - translationX, origin.y - translationY);

	}

	public void centerOnDisplayCoordinates(final Point p) {
		Point origin = getOrigin();
		centerOnViewCoordinates(new Point(p.x + origin.x, p.y + origin.y));
	}

	public void selectAgents(final int mousex, final int mousey) {
		Point origin = getOrigin();
		final int xc = mousex - origin.x;
		final int yc = mousey - origin.y;
		final List<ILayer> layers = manager.getLayersIntersecting(xc, yc);
		if ( layers.isEmpty() ) { return; }
		final ILocation modelCoordinates = layers.get(0).getModelCoordinatesFrom(xc, yc, this);
		scope.getGui().run(new Runnable() {

			@Override
			public void run() {
				menuManager.buildMenu(mousex, mousey, xc, yc, modelCoordinates, layers);
			}
		});
	}

	@Override
	public void layersChanged() {}

	/**
	 * Method changed()
	 * @see msi.gama.outputs.LayeredDisplayData.DisplayDataListener#changed(int, boolean)
	 */
	@Override
	public void changed(final Changes property, final boolean value) {

		switch (property) {
			case BACKGROUND:
				setBackground(getData().getBackgroundColor());
				break;
			default:;
		}

	};

	@Override
	public synchronized void acquireLock() {
		while (lockAcquired) {
			try {
				wait();
			} catch (final InterruptedException e) {
				// e.printStackTrace();
			}
		}
		lockAcquired = true;
	}

	@Override
	public synchronized void releaseLock() {
		lockAcquired = false;
		notify();
	}

}