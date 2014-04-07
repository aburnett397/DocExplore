package org.interreg.docexplore.authoring;

import java.io.File;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.apache.commons.io.FileUtils;
import org.interreg.docexplore.DocExploreTool;
import org.interreg.docexplore.management.DocExploreDataLink;
import org.interreg.docexplore.manuscript.Book;
import org.interreg.docexplore.util.ByteUtils;
import org.interreg.docexplore.util.ZipUtils;

public class WebExporter extends PresentationExporter
{
	File exportDir = new File(DocExploreTool.getHomeDir(), "web-tmp");
	
	public WebExporter(AuthoringToolFrame tool)
	{
		super(tool);
	}

	boolean copyComplete = false;
	public void doExport(final DocExploreDataLink link) throws Exception
	{
		copyComplete = false;
		ExportOptions options = ExportOptions.getOptions(tool);
		if (options == null)
			return;
		
		File exportTo = DocExploreTool.getFileDialogs().saveFile(DocExploreTool.getWebIBookCategory());
		if (exportTo == null)
			return;
		if (exportTo.exists() && 
			JOptionPane.showConfirmDialog(tool, "A file with the same name already exists. Do you wish to overwrite it?", "Overwrite", 
				JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
					return;
		
		exportDir.mkdirs();
		Book book = link.getBook(link.getLink().getAllBookIds().get(0));
		doExport(book, exportDir, options, 0);
		
		copyResource("org/interreg/docexplore/reader/web/index.html", exportDir);
		copyResource("org/interreg/docexplore/reader/web/back.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/empty.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/left.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/right.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/zoom.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/zoomin.png", exportDir);
		copyResource("org/interreg/docexplore/reader/web/zoomout.png", exportDir);
		File jsDir = new File(exportDir, "js");
		jsDir.mkdir();
		copyResource("org/interreg/docexplore/reader/web/js/Camera.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Hand.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Input.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Math3D.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Paper.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Reader.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Region.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/Specification.js", jsDir);
		copyResource("org/interreg/docexplore/reader/web/js/three.js", jsDir);
		copyComplete = true;
		
		ZipUtils.zip(exportDir, exportTo, progress);
		FileUtils.deleteDirectory(exportDir);
	}
	
	static void copyResource(String resource, File dir) throws Exception
	{
		File dest = new File(dir, resource.substring(resource.lastIndexOf('/')+1));
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		byte [] bytes = ByteUtils.readStream(stream);
		stream.close();
		ByteUtils.writeFile(dest, bytes);
	}
}