/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.management.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.interreg.docexplore.DocExploreTool;
import org.interreg.docexplore.Startup;
import org.interreg.docexplore.datalink.DataLink;
import org.interreg.docexplore.datalink.DataLinkException;
import org.interreg.docexplore.gui.ErrorHandler;
import org.interreg.docexplore.internationalization.XMLResourceBundle;
import org.interreg.docexplore.management.Clipboard;
import org.interreg.docexplore.management.DocExploreDataLink;
import org.interreg.docexplore.management.image.PageViewer;
import org.interreg.docexplore.management.manage.ManageComponent;
import org.interreg.docexplore.management.manage.ManageHandler;
import org.interreg.docexplore.management.plugin.PluginManager;
import org.interreg.docexplore.management.search.SearchComponent;
import org.interreg.docexplore.management.search.SearchHandler;
import org.interreg.docexplore.manuscript.AnnotatedObject;
import org.interreg.docexplore.manuscript.Book;
import org.interreg.docexplore.manuscript.Page;
import org.interreg.docexplore.manuscript.Region;
import org.interreg.docexplore.manuscript.actions.ActionProvider;
import org.interreg.docexplore.nfd.JNativeFileDialog;
import org.interreg.docexplore.util.GuiUtils;
import org.interreg.docexplore.util.history.HistoryManager;
import org.interreg.docexplore.util.history.HistoryPanel;

public class MainWindow extends JFrame
{
	public static interface MainWindowListener
	{
		public void activeDocumentChanged(AnnotatedObject document);
		public void dataLinkChanged(DocExploreDataLink link);
	}
	
	private static final long serialVersionUID = 6037278838028408126L;
	
	private DocExploreDataLink link;
	private ActionProvider actionProvider;
	
	// Contains all opened documents
	JTabbedPane tabbedPane;
	
	public SearchComponent searchComponent;
	public ManageComponent manageComponent;
	
	public final PluginManager pluginManager;
//	public final ProcessDialog processDialog;
//	public ExportDialog exportDialog;
	public final MainToolBar toolBar;
	MainStatusBar statusBar;
	MainMenuBar menuBar;
	JPanel centerPanel;
	ExecutorService service;
	public final Clipboard clipboard;
	public JNativeFileDialog chooser = new JNativeFileDialog();
	
	public HistoryManager historyManager;
	public JDialog historyDialog;
	
	public NotificationStack notifications;
	
	List<MainWindowListener> listeners;

	@SuppressWarnings("serial")
	public MainWindow(Startup startup, PluginManager pluginManager) throws FileNotFoundException, IOException, ClassNotFoundException
	{
		super(XMLResourceBundle.getBundledString("frameTitle"));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		this.listeners = new LinkedList<MainWindowListener>();
		this.link = new DocExploreDataLink();
		this.service = Executors.newFixedThreadPool(4);
		this.pluginManager = pluginManager;
		
		//UIManager.put("swing.boldMetal", Boolean.FALSE);

		this.tabbedPane = new JTabbedPane();
		
		this.getContentPane().setLayout(new BorderLayout());
		this.centerPanel = new JPanel(new BorderLayout());//new LooseGridLayout(1, 0, 1, 1, false, true, SwingConstants.LEFT, SwingConstants.TOP));
		this.getContentPane().add(centerPanel, BorderLayout.CENTER);
		
		startup.screen.setText("Initializing history");
		this.historyManager = new HistoryManager(50, new File(DocExploreTool.getHomeDir(), ".mmt-cache"));
		this.historyDialog = new JDialog(this, XMLResourceBundle.getBundledString("generalHistory"));
		historyDialog.add(new HistoryPanel(historyManager));
		historyDialog.pack();
		
		this.menuBar = new MainMenuBar(this);
		setJMenuBar(menuBar);
		
		startup.screen.setText("Initializing search");
		this.searchComponent = new SearchComponent(new SearchHandler(this));
		this.manageComponent = new ManageComponent(this, new ManageHandler(this));
		
//		startup.screen.setText("Initializing processing");
//		this.processDialog = new ProcessDialog();
//		try {this.exportDialog = new ExportDialog(null);}
//		catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
		
		centerPanel.add(tabbedPane, BorderLayout.CENTER);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
		
		tabbedPane.addChangeListener(new ChangeListener() {public void stateChanged(ChangeEvent e) {notifyActiveDocumentChanged();}});
		
		startup.screen.setText("Initializing toolbar");
		this.toolBar = new MainToolBar(this);
		this.getContentPane().add(toolBar, BorderLayout.NORTH);
		this.statusBar = new MainStatusBar(this);
		this.getContentPane().add(statusBar, BorderLayout.SOUTH);
		
		this.clipboard = new Clipboard();
		
		this.notifications = new NotificationStack(this);
		getLayeredPane().add(notifications, JLayeredPane.MODAL_LAYER);
		
		startup.screen.setText("Initializing analysis plugins");
		pluginManager.initAnalysisPlugins(this);
		
		notifyActiveDocumentChanged();
		notifyDataLinkChanged();

		pack();
		
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "LEFT");
		getRootPane().getActionMap().put("LEFT", new AbstractAction() {public void actionPerformed(ActionEvent e) {toolBar.prev.doClick();}});
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "RIGHT");
		getRootPane().getActionMap().put("RIGHT", new AbstractAction() {public void actionPerformed(ActionEvent e) {toolBar.next.doClick();}});
		getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "UP");
		getRootPane().getActionMap().put("UP", new AbstractAction() {public void actionPerformed(ActionEvent e) {toolBar.up.doClick();}});
	}
	
	public DocExploreDataLink getDocExploreLink() {return link;}
	public ActionProvider getActionProvider() {return actionProvider;}
	public void setLink(DataLink link) throws DataLinkException
	{
		//System.out.println("set link with "+link+" - currently "+(this.link!=null ? this.link.getLink() : null));
		this.link.setLink(link);
		this.actionProvider = link == null ? null : link.getActionProvider(this.link);
		tabbedPane.removeAll();
		resetComponents();
		
		notifyDataLinkChanged();
	}
	
	public void resetComponents()
	{
		GuiUtils.blockUntilComplete(new Runnable()
		{
			public void run()
			{
				addLeftPanel(null, 0);
				manageComponent = new ManageComponent(MainWindow.this, new ManageHandler(MainWindow.this));
				searchComponent = new SearchComponent(new SearchHandler(MainWindow.this));
//				try {exportDialog = new ExportDialog(link);}
//				catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
				historyManager.reset(link.getLink() != null && link.getLink().supportsHistory() ? 20 : -1);
				getContentPane().invalidate();
				getContentPane().validate();
				repaint();
			}
		}, tabbedPane);
	}
	
	public void addMainWindowListener(MainWindowListener listener) {listeners.add(listener);}
	public void removeMainWindowListener(MainWindowListener listener) {listeners.remove(listener);}
	void notifyActiveDocumentChanged()
	{
		DocumentPanel panel = getActiveTab();
		AnnotatedObject document = panel != null ? panel.document : null;
		for (MainWindowListener listener : listeners)
			listener.activeDocumentChanged(document);
	}
	void notifyDataLinkChanged()
	{
		for (MainWindowListener listener : listeners)
			listener.dataLinkChanged(link);
	}
	
	public void addLeftPanel(Component panel, double relw)
	{
		Component prev = ((BorderLayout)centerPanel.getLayout()).getLayoutComponent(BorderLayout.WEST);
		if (prev != null && prev != panel)
			centerPanel.remove(prev);
		if (panel != null)
		{
			//panel.setPreferredSize(new Dimension((int)(relw*getWidth()), centerPanel.getHeight()-centerPanel.getInsets().bottom-centerPanel.getInsets().top));
			centerPanel.add(panel, BorderLayout.WEST);
		}
		centerPanel.validate();
		centerPanel.invalidate();
		repaint();
	}
	
	public void removeLeftPanel(Component panel)
	{
		centerPanel.remove(panel);
		centerPanel.validate();
		centerPanel.invalidate();
		repaint();
	}
	
	/**
	 * Remove a tab by searching his id into tabbedPane
	 * @param index
	 * @throws IllegalArgumentException if index = -1
	 */
	public void removeTab(int index) throws IllegalArgumentException
	{
		try{tabbedPane.remove(index);}
		catch(IndexOutOfBoundsException e) {ErrorHandler.defaultHandler.submit(e);}
	}
	
	public DocumentPanel addTab(final AnnotatedObject document) throws Exception
	{
		DocumentPanel panel = new DocumentPanel(MainWindow.this, (DocExploreDataLink)document.getLink());
		tabbedPane.add("", panel);
		return setTabDocument(tabbedPane.getTabCount()-1, document, true);
			
	}
	
	public void closeBooks(Collection<Book> books)
	{
		for (int i=tabbedPane.getTabCount()-1;i>=0;i--)
		{
			AnnotatedObject object = ((DocumentPanel)tabbedPane.getComponentAt(i)).getDocument();
			Book book = object instanceof Region ? ((Region)object).getPage().getBook() : 
				object instanceof Page ? ((Page)object).getBook() :
				object instanceof Book ? (Book)object : null;
			if (book != null)
				for (Book tbook : books)
					if (tbook.getId() == book.getId())
						{tabbedPane.removeTabAt(i); break;}
		}
	}
	
	public void closePages(Collection<Page> pages)
	{
		for (int i=tabbedPane.getTabCount()-1;i>=0;i--)
		{
			AnnotatedObject object = ((DocumentPanel)tabbedPane.getComponentAt(i)).getDocument();
			Page page = object instanceof Region ? ((Region)object).getPage() : object instanceof Page ? (Page)object : null;
			if (page != null && pages.contains(page))
				tabbedPane.removeTabAt(i);
		}
	}
	
	public void refreshTabNames()
	{
		for (int i=tabbedPane.getTabCount()-1;i>=0;i--)
		{
			AnnotatedObject document = ((DocumentPanel)tabbedPane.getComponentAt(i)).getDocument();
			((DocumentTab)tabbedPane.getTabComponentAt(i)).setTitle(getTabName(document));
		}
	}
	
	public void refreshTabs()
	{
		for (int i=tabbedPane.getTabCount()-1;i>=0;i--)
			try {((DocumentPanel)tabbedPane.getComponentAt(i)).refresh();}
			catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
	}
	
	public DocumentPanel getPanelForDocument(AnnotatedObject document)
	{
		for (int i=0;i<tabbedPane.getTabCount();i++)
			if (((DocumentPanel)tabbedPane.getComponentAt(i)).getDocument() == document)
				return (DocumentPanel)tabbedPane.getComponentAt(i);
		return null;
	}
	public DocumentPanel getPanelForPage(Page page)
	{
		for (int i=0;i<tabbedPane.getTabCount();i++)
		{
			AnnotatedObject doc = ((DocumentPanel)tabbedPane.getComponentAt(i)).getDocument();
			if (doc == page || (doc instanceof Region && ((Region)doc).getPage() == page))
				return (DocumentPanel)tabbedPane.getComponentAt(i);
		}
		return null;
	}
	
	public String getTabName(AnnotatedObject document)
	{
		String title = null;
		if (document instanceof Page || document instanceof Region)
		{
			Page page = document instanceof Page ? (Page)document :
				((Region)document).getPage();
			title = page.getBook().getName()+" p"+page.getPageNumber();
			if (document instanceof Region)
				title += " (ROI)";
		}
		else if (document instanceof Book)
			title = ((Book)document).getName();
		return title;
	}
	
	public DocumentPanel setTabDocument(int index, final AnnotatedObject document, boolean reset) throws Exception
	{
		AnnotatedObject alreadyOpen = null;
		for (int i=0;i<tabbedPane.getTabCount();i++)
		{
			DocumentPanel panel = (DocumentPanel)tabbedPane.getComponentAt(i);
			if (i != index && panel.getDocument() == document || DocExploreDataLink.isSamePage(panel.getDocument(), document))
			{
				if (i != index)
				{
					((DocumentPanel)tabbedPane.getComponentAt(index)).documentIsClosing(null);
					tabbedPane.removeTabAt(index);
					if (i > index) i--;
				}
				index = i;
				alreadyOpen = ((DocumentPanel)tabbedPane.getComponentAt(index)).document;
				break;
			}
		}
		
		final DocumentPanel panel = (DocumentPanel)tabbedPane.getComponentAt(index);
		if (panel.document != null && panel.document != document)
			panel.documentIsClosing(document);
		String title = getTabName(document);
		if (title == null)
			throw new Exception("Cannot open tab for object ("+document.getClass().getName()+")");
		
		tabbedPane.setTabComponentAt(index, new DocumentTab(title, MainWindow.this.tabbedPane));
		
		if (alreadyOpen == null)
		{
			if (reset)
				panel.setDividerLocation(.5);
			
			final JDialog dialog = new JDialog(this, true);
			dialog.setUndecorated(true);
			JProgressBar progress = new JProgressBar(JProgressBar.HORIZONTAL);
			progress.setIndeterminate(true);
			dialog.add(new JLabel(XMLResourceBundle.getBundledString("manageLoadingLabel")), BorderLayout.NORTH);
			dialog.add(progress, BorderLayout.SOUTH);
			dialog.pack();
			GuiUtils.centerOnScreen(dialog);
			
			new Thread() {public void run()
			{
				try {panel.fillPanels(document);}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e);}
				
				while (!dialog.isVisible())
					try {Thread.sleep(100);}
					catch (Exception e) {}
				dialog.setVisible(false);
			}}.start();
			
			dialog.setVisible(true);
		}
		else if (DocExploreDataLink.isSamePage(document, alreadyOpen))
		{
			double zoom = panel.pageViewer.transform.getScaleX();
			Point ori = panel.viewerScrollPane.getViewport().getViewPosition();
			PageViewer.ImageOperation operation = panel.pageViewer.getOperation();
			
			panel.fillPanels(document);
			
			panel.pageViewer.setZoom(zoom);
			panel.viewerScrollPane.getViewport().setViewPosition(ori);
			panel.pageViewer.setOperation(operation);
		}
		tabbedPane.setSelectedIndex(index);
		tabbedPane.validate();
		tabbedPane.repaint();
		return (DocumentPanel)tabbedPane.getComponentAt(index);
	}
	
	public DocumentPanel setActiveTabDocument(AnnotatedObject document) throws Exception
	{
		return setTabDocument(tabbedPane.getSelectedIndex(), document, false);
	}
	
	public DocumentPanel getActiveTab()
	{
		return (DocumentPanel)tabbedPane.getSelectedComponent();
	}
	
	public void close()
	{
		Component [] comps = tabbedPane.getComponents();
		for (int i=0;i<comps.length;i++)
			if (comps[i] instanceof DocumentPanel)
				((DocumentPanel)comps[i]).annotationPanel.contractAllAnnotations();
		processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		setVisible(false);
	}
}
