/*******************************************************************************
 * Copyright (c) 2012 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.visualizer.examples;

import org.eclipse.cdt.visualizer.ui.canvas.GraphicCanvas;
import org.eclipse.cdt.visualizer.ui.canvas.GraphicCanvasVisualizer;
import org.eclipse.cdt.visualizer.ui.canvas.GraphicObject;
import org.eclipse.cdt.visualizer.ui.util.Colors;
import org.eclipse.cdt.visualizer.ui.util.SelectionManager;
import org.eclipse.cdt.visualizer.ui.util.SelectionUtils;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;

public class ProblemVisualizer extends GraphicCanvasVisualizer {

	/** The width of the side margins */
	private static final int MARGIN_WIDTH = 10;
	/** The height of the top and bottom margins */
	private static final int MARGIN_HEIGHT = 10;
	/** The default space between bars in the chart */
	private static final int SPACING_HEIGHT = 40;
	/** The predefined number of severities */
	private static final int NUM_SEVERITY = 3;

	/* The different colors to use for the different severities */
	private static final Color ERROR_OUTLINE_COLOR = Colors.DARK_RED;
	private static final Color ERROR_INSIDE_COLOR = Colors.DARK_RED;
	private static final Color WARNING_OUTLINE_COLOR = Colors.DARK_YELLOW;
	private static final Color WARNING_INSIDE_COLOR = Colors.DARK_YELLOW;
	private static final Color INFO_OUTLINE_COLOR = Colors.DARK_BLUE;
	private static final Color INFO_INSIDE_COLOR = Colors.DARK_BLUE;
	
	private static final Color MAIN_BACKGROUND_COLOR = Colors.WHITE;
	private static final Color MAIN_FOREGROUND_COLOR = Colors.BLACK;

	/**
	 * A class that draws a bar or a bar outline in the specified color.
	 */
	private class BarGraphicObject extends GraphicObject {
		private boolean m_outline;
		
		public BarGraphicObject(Color color, int x, int y, int w, int h, boolean outline) {
			super(x, y, w, h);
			m_outline = outline;
			
			if (m_outline) {
				setForeground(color);
			} else {
				setBackground(color);
			}
		}
		
		@Override
		public void paintContent(GC gc) {
			if (m_outline) {
				gc.drawRectangle(m_bounds);
			} else {
				gc.fillRectangle(m_bounds);
			}
		}
	}
	
	/** The canvas on which we'll draw our bars */
	private GraphicCanvas m_canvas;
	
	/**
	 * The model containing the data to be displayed.
	 * In this case, it is the number of the three
	 * different types of problem markers.
	 */
	private int[] m_markerCount = new int[NUM_SEVERITY];
	
	public ProblemVisualizer() {
		super();
	}

	@Override
	public String getName() {
		return Messages.ProblemCountVisualizer_Name;
	}
	
	@Override
	public String getDisplayName() {
		return Messages.ProblemCountVisualizer_DisplayName;
	}
	
	@Override
	public String getDescription() {
		return Messages.ProblemCountVisualizer_Description;
	}
	
	@Override
	public GraphicCanvas createCanvas(Composite parent) {
		m_canvas = new GraphicCanvas(parent);
		return m_canvas;
	}
	
	@Override
	protected void initializeCanvas(GraphicCanvas canvas) {		
		m_canvas.setBackground(MAIN_BACKGROUND_COLOR);
		m_canvas.setForeground(MAIN_FOREGROUND_COLOR);
	}
	
	@Override
	public int handlesSelection(ISelection selection) {
		Object sel = SelectionUtils.getSelectedObject(selection);

		if (sel instanceof IResource) {
			return 2;
		}
		
		return 0;
	}
	
	@Override
	public void visualizerDeselected() {
	}
	
	@Override
	public void visualizerSelected() {
	}

	/**
	 * Actually create the graphics bars for the different severities.
	 * @param outline Should the bars be created, or the bar outline
	 * @return The bars to be drawn.
	 */
	private BarGraphicObject[] getBars(boolean outline) {
		BarGraphicObject[] bars = new BarGraphicObject[3];
		
		Rectangle bounds = m_canvas.getBounds();

		int x = bounds.x + MARGIN_WIDTH;
		int y = bounds.y + MARGIN_HEIGHT;
		
		int spacing = SPACING_HEIGHT;
		int height = (bounds.height - 2 * MARGIN_HEIGHT - 2 * SPACING_HEIGHT) / 3;
		if (height <= 0) {
			spacing = 0;
			y = bounds.y;
			height = bounds.height / 3;
		}

		int maxWidth = bounds.width - 2 * MARGIN_WIDTH;
		
		if (outline) {
			// The bar outlines take the entire width of the view
			bars[0] = new BarGraphicObject(ERROR_OUTLINE_COLOR, x, y, maxWidth, height, outline);
			
			y = y + height + spacing;
			bars[1] = new BarGraphicObject(WARNING_OUTLINE_COLOR, x, y, maxWidth, height, outline);

			y = y + height + spacing;
			bars[2] = new BarGraphicObject(INFO_OUTLINE_COLOR, x, y, maxWidth, height, outline);

		} else {
			// The inside of the bars use a proportional width with the maximum width and
			// the largest amount of markers for one severity.
			
			// Find the maximum marker count to dictate the width
			int maxCount = Math.max(m_markerCount[0], m_markerCount[1]);
			maxCount = Math.max(maxCount, m_markerCount[2]);
			if (maxCount == 0) maxCount = 1; // Set to anything but 0.  It will be multiplied by 0 and not matter.

			int width = maxWidth * m_markerCount[IMarker.SEVERITY_ERROR] / maxCount;
			bars[0] = new BarGraphicObject(ERROR_INSIDE_COLOR, x, y, width, height, outline);

			y = y + height + spacing;
			width = maxWidth * m_markerCount[IMarker.SEVERITY_WARNING] / maxCount;
			bars[1] = new BarGraphicObject(WARNING_INSIDE_COLOR, x, y, width, height, outline);

			y = y + height + spacing;
			width = maxWidth * m_markerCount[IMarker.SEVERITY_INFO] / maxCount;
			bars[2] = new BarGraphicObject(INFO_INSIDE_COLOR, x, y, width, height, outline);
		}
		
		return bars;
	}
	
	/**
	 * Get the count of problem markers for each severity for the
	 * specified resource.
	 */
	private void setMarkerCount(IResource resource) {
		m_markerCount[IMarker.SEVERITY_ERROR] = 0;
		m_markerCount[IMarker.SEVERITY_WARNING] = 0;
		m_markerCount[IMarker.SEVERITY_INFO] = 0;

		IMarker[] problems = null;
		try {
			problems = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			return;
		}
		
		for (IMarker problem : problems) {
			try {
				Object attrValue = problem.getAttribute(IMarker.SEVERITY);
				if (attrValue != null && attrValue instanceof Integer) {
					int severity = (Integer)attrValue;
					m_markerCount[severity]++;
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void workbenchSelectionChanged(ISelection selection) {
		m_canvas.clear();
		
		// First create the outline bars
		BarGraphicObject[] bars = getBars(true);
		for (BarGraphicObject bar : bars) {
			m_canvas.add(bar);
		}
		
		Object sel = SelectionUtils.getSelectedObject(selection);
		if (sel instanceof IResource) {
			// Now, create the inside bars

			setMarkerCount((IResource)sel);

			bars = getBars(false);
			for (BarGraphicObject bar : bars) {
				m_canvas.add(bar);
			}
		}
		
		m_canvas.redraw();
	}
	
	public SelectionManager getSelectionManager() {
		return m_selectionManager;
	}	
}