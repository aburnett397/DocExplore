/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.authoring.explorer.edit;

import java.awt.FlowLayout;
import java.awt.Point;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.interreg.docexplore.gui.ErrorHandler;
import org.interreg.docexplore.gui.WrapLayout;
import org.interreg.docexplore.internationalization.XMLResourceBundle;
import org.interreg.docexplore.management.gui.ToolbarButton;
import org.interreg.docexplore.management.gui.ToolbarToggleButton;
import org.interreg.docexplore.management.image.CropOperation;
import org.interreg.docexplore.management.image.FreeShapeOperation;
import org.interreg.docexplore.management.image.SquareOperation;
import org.interreg.docexplore.manuscript.AnnotatedObject;
import org.interreg.docexplore.manuscript.Page;
import org.interreg.docexplore.manuscript.Region;

@SuppressWarnings("serial")
public class PageEditorToolbar extends JPanel
{
	public PageEditorToolbar(final PageEditor editor, final JScrollPane scrollPane)
	{
		super(new WrapLayout(FlowLayout.LEFT));
		
		editor.addMainWindowListener((ToolbarButton)add(new ToolbarButton("previous-24x24.png", XMLResourceBundle.getBundledString("imageToolbarPreviousHistoric"))
		{
			public void clicked()
			{
				try
				{
					final int pageNum = editor.view.curPage.getPageNumber();
					if (pageNum < 2)
						return;
					final Page page = editor.view.curPage.getBook().getPage(pageNum-1);
					SwingUtilities.invokeLater(new Runnable() {public void run()
					{
						editor.view.explorer.explore(page.getBook().getCanonicalUri());
						editor.view.explorer.explore(page.getCanonicalUri());
					}});
				}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
			}
		}));
		editor.addMainWindowListener((ToolbarButton)add(new ToolbarButton("next-24x24.png", XMLResourceBundle.getBundledString("imageToolbarNextHistoric"))
		{
			public void clicked()
			{
				try
				{
					final int pageNum = editor.view.curPage.getPageNumber();
					if (pageNum > editor.view.curPage.getBook().getLastPageNumber()-1)
						return;
					final Page page = editor.view.curPage.getBook().getPage(pageNum+1);
					SwingUtilities.invokeLater(new Runnable() {public void run()
					{
						editor.view.explorer.explore(page.getBook().getCanonicalUri());
						editor.view.explorer.explore(page.getCanonicalUri());
					}});
				}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
			}
		}));
		
		final double zoomFactor = 1.5;
		editor.addMainWindowListener((ToolbarButton)add(new ToolbarButton("zoom-in-24x24.png", XMLResourceBundle.getBundledString("imageToolbarZoomIn"))
		{
			public void clicked()
			{
				Point point = scrollPane.getViewport().getViewPosition();
				editor.applyZoomShift(zoomFactor);
				point.x *= zoomFactor; point.y *= zoomFactor;
				scrollPane.getViewport().setViewPosition(point);
			}
		}));
		editor.addMainWindowListener((ToolbarButton)add(new ToolbarButton("zoom-out-24x24.png", XMLResourceBundle.getBundledString("imageToolbarZoomOut"))
		{
			public void clicked()
			{
				Point point = scrollPane.getViewport().getViewPosition();
				editor.applyZoomShift(1/zoomFactor);
				point.x /= zoomFactor; point.y /= zoomFactor;
				scrollPane.getViewport().setViewPosition(point);
			}
		}));
		
		ToolbarButton fit = new ToolbarButton("fit-24x24.png", XMLResourceBundle.getBundledString("imageToolbarFit"))
		{
			public void clicked() {editor.fit();}
		};
		add(fit);
		editor.addMainWindowListener(fit);
		
		add(new JSeparator(SwingConstants.VERTICAL));
		
		final List<ToolbarToggleButton> roiButtons = new LinkedList<ToolbarToggleButton>();
		
		ToolbarToggleButton addFree = new ToolbarToggleButton("add-free-roi-24x24.png", XMLResourceBundle.getBundledString("imageToolbarAddFreeRoi"))
		{
			public void toggled()
			{				
				for (ToolbarToggleButton button : roiButtons)
					if (button != this)
						button.setSelected(false);
				editor.setOperation(new FreeShapeOperation());
			}
			public void untoggled()
			{
				editor.setOperation(null);
			}
		};
		roiButtons.add((ToolbarToggleButton)add(addFree));
		editor.addMainWindowListener(addFree);
		
		ToolbarToggleButton addRect = new ToolbarToggleButton("add-rect-roi-24x24.png", XMLResourceBundle.getBundledString("imageToolbarAddRectRoi"))
		{
			public void toggled()
			{				
				for (ToolbarToggleButton button : roiButtons)
					if (button != this)
						button.setSelected(false);
				editor.setOperation(new SquareOperation());
			}
			public void untoggled()
			{
				editor.setOperation(null);
			}
		};
		roiButtons.add((ToolbarToggleButton)add(addRect));
		editor.addMainWindowListener(addRect);
		
		editor.addMainWindowListener((ToolbarButton)add(new ToolbarButton("del-roi-24x24.png", XMLResourceBundle.getBundledString("imageToolbarDelRoi"))
		{
			public void clicked()
			{
				if (editor.document != null && editor.document instanceof Region)
					editor.deleteRegion((Region)editor.document);
			}
			public void activeDocumentChanged(AnnotatedObject document)
			{
				setEnabled(document != null && document instanceof Region);
			}
		}));
		
		ToolbarToggleButton crop = new ToolbarToggleButton("resize-page-24x24.png", XMLResourceBundle.getBundledString("imageToolbarCrop"))
		{
			public void toggled()
			{				
				for (ToolbarToggleButton button : roiButtons)
					if (button != this)
						button.setSelected(false);
				editor.setOperation(new CropOperation());
			}
			public void untoggled()
			{
				editor.setOperation(null);
			}
		};
		roiButtons.add((ToolbarToggleButton)add(crop));
		editor.addMainWindowListener(crop);
	}
}
