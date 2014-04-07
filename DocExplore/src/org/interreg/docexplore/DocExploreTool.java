package org.interreg.docexplore;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import org.interreg.docexplore.gui.ErrorHandler;
import org.interreg.docexplore.gui.FileDialogs;
import org.interreg.docexplore.gui.LooseGridLayout;
import org.interreg.docexplore.internationalization.XMLResourceBundle;
import org.interreg.docexplore.nfd.JNativeFileDialog;
import org.interreg.docexplore.util.GuiUtils;
import org.interreg.docexplore.util.ImageUtils;
import org.interreg.docexplore.util.StringUtils;
import org.interreg.docexplore.util.ZipUtils;

public class DocExploreTool
{
	private static File homeDir = null, execDir = null, pluginDir = null;
	static {initDirectories();}
	
	public static File getHomeDir() {return homeDir;}
	public static File getExecutableDir() {return execDir;}
	public static File getPluginDir() {return pluginDir;}
	
	public static File initDirectories()
	{
		execDir = new File(".").getAbsoluteFile();
		System.out.println("Executable dir: "+execDir.getAbsolutePath());
		
		pluginDir = new File(System.getProperty("user.home")+File.separator+"DocExplorePlugins");
		if (!pluginDir.exists())
		{
			pluginDir.mkdirs();
			File pluginArchive = new File(execDir, "plugins.zip");
			if (pluginArchive.exists())
			{
				try {ZipUtils.unzip(pluginArchive, pluginDir);}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e, true);}
			}
		}
		System.out.println("Plugins dir: "+pluginDir.getAbsolutePath());
		
		System.setProperty("file.encoding", "UTF-8");
		
		System.out.println("OS: "+System.getProperty("os.name"));
		System.out.println("Arch: "+System.getProperty("os.arch"));
		System.out.println("Java: "+System.getProperty("java.version"));
		System.out.println("Encoding: "+System.getProperty("file.encoding"));
		System.out.println("PID: "+Uninstaller.getPID());
		
		try {UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}
		catch (Exception e) {e.printStackTrace();}
		
		final File [] file = {null};
		List<String> homes = getHomes();
		if (homes != null)
			file[0] = new File(homes.get(0));
		if (file[0] == null)
		{
			//JOptionPane.showMessageDialog(null, "Test the default font too see the difference.\nAnd the line spacing.");
			file[0] = askForHome(XMLResourceBundle.getBundledString("chooseHomeMessage"));
			if (file[0] == null)
				System.exit(0);
		}
		
		try
		{
			setHome(file[0].getCanonicalPath());
			initIfNecessary(file[0]);
		}
		catch (Exception e)
		{
			ErrorHandler.defaultHandler.submit(e); 
			if (homeDir == null) 
				System.exit(0);
		}
		
		return file[0];
	}
	
	@SuppressWarnings("serial")
	protected static File askForHome(String text)
	{
		final File [] file = {null};
		final JDialog dialog = new JDialog((Frame)null, XMLResourceBundle.getBundledString("homeLabel"), true);
		JPanel content = new JPanel(new LooseGridLayout(0, 1, 10, 10, true, false, SwingConstants.CENTER, SwingConstants.TOP, true, false));
		content.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		JLabel message = new JLabel(text, ImageUtils.getIcon("free-64x64.png"), SwingConstants.LEFT);
		message.setIconTextGap(20);
		//message.setFont(Font.decode(Font.SANS_SERIF));
		content.add(message);
		
		final JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		pathPanel.add(new JLabel("<html><b>"+XMLResourceBundle.getBundledString("homeLabel")+":</b></html>"));
		final JTextField pathField = new JTextField(System.getProperty("user.home")+File.separator+"DocExplore", 40);
		pathPanel.add(pathField);
		pathPanel.add(new JButton(new AbstractAction(XMLResourceBundle.getBundledString("browseLabel"))
		{
			JNativeFileDialog nfd = null;
			public void actionPerformed(ActionEvent arg0)
			{
				if (nfd == null)
				{
					nfd = new JNativeFileDialog();
					nfd.acceptFiles = false;
					nfd.acceptFolders = true;
					nfd.multipleSelection = false;
					nfd.title = XMLResourceBundle.getBundledString("homeLabel");
				}
				nfd.setCurrentFile(new File(pathField.getText()));
				if (nfd.showOpenDialog())
					pathField.setText(nfd.getSelectedFile().getAbsolutePath());
			}
		}));
		content.add(pathPanel);
		
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		buttonPanel.add(new JButton(new AbstractAction(XMLResourceBundle.getBundledString("cfgOkLabel")) {public void actionPerformed(ActionEvent e)
		{
			File res = new File(pathField.getText());
			if (res.exists() && !res.isDirectory() || !res.exists() && !res.mkdirs())
				JOptionPane.showMessageDialog(dialog, XMLResourceBundle.getBundledString("homeErrorMessage"), XMLResourceBundle.getBundledString("errorLabel"), JOptionPane.ERROR_MESSAGE);
			else
			{
				file[0] = res;
				dialog.setVisible(false);
			}
		}}));
		buttonPanel.add(new JButton(new AbstractAction(XMLResourceBundle.getBundledString("cfgCancelLabel")) {public void actionPerformed(ActionEvent e) {dialog.setVisible(false);}}));
		content.add(buttonPanel);
		
		dialog.getContentPane().add(content);
		dialog.pack();
		dialog.setResizable(false);
		GuiUtils.centerOnScreen(dialog);
		dialog.setVisible(true);
		return file[0];
	}
	
	@SuppressWarnings("unchecked")
	protected static void setHome(String value)
	{
		File homePointerFile = new File(System.getProperty("user.home")+"/.docexplore");
		if (homePointerFile.exists())
		{
			List<String> homes = null;
			try
			{
				ObjectInputStream in = new ObjectInputStream(new FileInputStream(homePointerFile));
				homes = (List<String>)in.readObject();
				in.close();
			}
			catch (Exception e)
			{
				ErrorHandler.defaultHandler.submit(e, true);
				try {homePointerFile.delete();}
				catch (Exception e2) {e2.printStackTrace();}
			}	
			
			if (homes != null)
			{
				for (int i=0;i<homes.size();i++)
					if (homes.get(i).equals(value))
						homes.remove(i--);
				homes.add(0, value);
				
				try
				{
					ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(homePointerFile, false));
					out.writeObject(homes);
					out.close();
				}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e, true);}
			}
		}
		if (!homePointerFile.exists())
		{
			try
			{
				ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(homePointerFile, false));
				List<String> homes = new LinkedList<String>();
				homes.add(value);
				out.writeObject(homes);
				out.close();
			}
			catch (Exception e) {ErrorHandler.defaultHandler.submit(e, true);}
		}
		homeDir = new File(value);
		System.out.println("DocExplore home directory: "+value);
	}
	
	@SuppressWarnings("unchecked")
	protected static List<String> getHomes()
	{
		File homePointerFile = new File(System.getProperty("user.home")+"/.docexplore");
		if (!homePointerFile.exists())
			return null;
		try
		{
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(homePointerFile));
			List<String> homes = (List<String>)in.readObject();
			in.close();
			return homes;
		}
		catch (Exception e) {ErrorHandler.defaultHandler.submit(e, true);}
		return null;
	}
	
	private static void initIfNecessary(File home) throws Exception
	{
		File readerDir = new File(home, "reader");
		File readerIndexFile = new File(readerDir, "index.xml");
		File configFile = new File(home, "config.xml");
		File dbFile = new File(home, "db");
		if (!readerDir.exists() || !readerIndexFile.exists() || !configFile.exists() || !dbFile.exists())
		{
			File initArchive = new File(execDir, "init.zip");
			if (initArchive.exists())
			{
				try {ZipUtils.unzip(initArchive, home);}
				catch (Exception e) {ErrorHandler.defaultHandler.submit(e, true);}
			}
			
			if (!readerDir.exists())
				readerDir.mkdir();
			if (!readerIndexFile.exists())
				StringUtils.writeFile(readerIndexFile, "<Index>\n</Index>");
			if (!configFile.exists())
				StringUtils.writeFile(configFile, "<config>\n\t<autoconnect>\n\t\t<use>yes</use>\n\t\t<type>file</type>\n\t\t<path>db</path>\n\t</autoconnect>\n</config>");
			if (!dbFile.exists())
				dbFile.mkdir();
		}
	}
	
	private static FileDialogs fileDialogs = null;
	public static FileDialogs.Category getImagesCategory() {return getFileDialogs().getOrCreateCategory("Images", ImageUtils.supportedFormats);}
	public static FileDialogs.Category getPluginCategory() {return getFileDialogs().getOrCreateCategory("DocExplore plugins", Collections.singleton("jar"));}
	public static FileDialogs.Category getIBookCategory() {return getFileDialogs().getOrCreateCategory("DocExplore Interactive Book", Collections.singleton("dib"));}
	public static FileDialogs.Category getWebIBookCategory() {return getFileDialogs().getOrCreateCategory("DocExplore Interactive Web Book", Collections.singleton("zip"));}
	public static FileDialogs.Category getPresentationCategory() {return getFileDialogs().getOrCreateCategory("DocExplore Presentation", Collections.singleton("pres"));}
	public static FileDialogs.Category getBookCategory() {return getFileDialogs().getOrCreateCategory("DocExplore Book", Collections.singleton("dmb"));}
	public static FileDialogs getFileDialogs()
	{
		if (fileDialogs == null || !fileDialogs.fdcache.getParentFile().equals(homeDir))
			fileDialogs = new FileDialogs(homeDir);
		return fileDialogs;
	}
	
}