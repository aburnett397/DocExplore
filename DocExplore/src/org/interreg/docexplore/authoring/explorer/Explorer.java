/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.authoring.explorer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.interreg.docexplore.authoring.AuthoringToolFrame;
import org.interreg.docexplore.gui.ErrorHandler;
import org.interreg.docexplore.gui.WrapLayout;
import org.interreg.docexplore.internationalization.XMLResourceBundle;
import org.interreg.docexplore.util.GuiUtils;
import org.interreg.docexplore.util.ImageUtils;

@SuppressWarnings("serial")
public class Explorer extends JPanel
{
	public static interface Listener
	{
		public void exploringChanged(Object explored);
	}
	
	public final AuthoringToolFrame tool;
	
	protected JLabel titleLabel;
	public List<ExplorerView> views;
	public JScrollPane scrollPane;
	protected JTextField pathField;
	public String curPath;
	public boolean iconMode = true;
	ExplorerView curView;
	public JPanel toolPanel;
	JButton upButton;
	
	public Explorer(AuthoringToolFrame tool) throws Exception
	{
		super(new BorderLayout());
		
		this.tool = tool;
		this.views = new Vector<ExplorerView>();
		this.curView = null;
		this.scrollPane = new JScrollPane(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		add(scrollPane, BorderLayout.CENTER);
		scrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {public void adjustmentValueChanged(AdjustmentEvent e)
		{
			if (scrollPane.getViewport().getView() != null)
				scrollPane.getViewport().getView().repaint();
		}});
		scrollPane.getHorizontalScrollBar().addAdjustmentListener(new AdjustmentListener() {public void adjustmentValueChanged(AdjustmentEvent e)
		{
			if (scrollPane.getViewport().getView() != null)
				scrollPane.getViewport().getView().repaint();
		}});
		
		this.toolPanel = new JPanel(new WrapLayout(FlowLayout.LEFT));
		toolPanel.add(titleLabel = new JLabel(XMLResourceBundle.getBundledString("generalPathLabel")));
		toolPanel.add(pathField = new JTextField(40));
		pathField.addKeyListener(new KeyAdapter()
		{
			@Override public void keyTyped(KeyEvent e)
			{
				if (e.getKeyChar() == '\n')
					explore(pathField.getText());
			}
		});
		
		toolPanel.add(upButton = new JButton(new AbstractAction("", ImageUtils.getIcon("up-24x24.png"))
		{
			@Override public void actionPerformed(ActionEvent arg0)
			{
				String parent = getParentPath(curPath);
				explore(parent == null ? "" : parent);
			}
		}) {{setToolTipText(XMLResourceBundle.getBundledString("generalToolbarParent"));}});
		toolPanel.add(new JToggleButton(new AbstractAction("", ImageUtils.getIcon("list-24x24.png"))
		{
			@Override public void actionPerformed(ActionEvent e)
			{
				iconMode = !((JToggleButton)e.getSource()).isSelected();
				curView.updateLayout();
			}
		}) {{setToolTipText(XMLResourceBundle.getBundledString("generalToolbarView"));}});
		add(toolPanel, BorderLayout.NORTH);
	}
	
	List<Listener> listeners = new LinkedList<Listener>();
	public void addListener(Listener listener) {listeners.add(listener);}
	public void removeListener(Listener listener) {listeners.remove(listener);}
	public void notifyExploringChanged(final Object explored)
	{
		GuiUtils.blockUntilComplete(new Runnable() {public void run()
		{
			for (Listener listener : listeners)
				listener.exploringChanged(explored);
		}}, this);
	}
	
	public String getParentPath(String path) {return path;}
	public void addView(ExplorerView view) {views.add(view);}
	
	public void explore(final String path)
	{
		GuiUtils.blockUntilComplete(new Runnable() {public void run()
		{
			try
			{
				for (ExplorerView view : views)
					if (view.canHandle(path))
				{
					view.setPath(path);
					scrollPane.setViewportView(view);
					curPath = path;
					pathField.setText(pathToString(path));
					validate();
					if (curView != null && view != curView)
					{
						curView.hidden();
						if (view != null)
							view.shown();
					}
					curView = view;
					String parentPath = getParentPath(curPath);
					parentPath = parentPath == null ? "" : parentPath;
					upButton.setEnabled(!parentPath.equals(curPath));
					return;
				}
				throw new Exception("Unable to handle path: '"+path+"'");
			}
			catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
		}}, this);
	}
	
	public void refreshPath() {pathField.setText(pathToString(curPath));}
	String pathToString(String path) {return path;}
	
	public void setCustomView(Component view)
	{
		scrollPane.setViewportView(view);
		scrollPane.validate();
	}
}
