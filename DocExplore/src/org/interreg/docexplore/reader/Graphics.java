/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.reader;

import java.awt.image.BufferedImage;

import org.interreg.docexplore.reader.gfx.Texture;

public interface Graphics
{
	public static interface Renderable
	{
		public void render(Graphics g);
	}
	
	public int width();
	public int height();
	
	public void setColor(float r, float g, float b, float a);
	public void drawLine(double x1, double y1, double x2, double y2);
	public void setWidth(float f);
	public void fillTriangle(double x1, double y1, double x2, double y2, double x3, double y3);
	
	public void addImage(BufferedImage image, double x, double y, double w, double h);
	public void removeImage(BufferedImage image);
	
	public void drawTriangle(double x1, double y1, double x2, double y2, double x3, double y3);
	public void drawRect(double x1, double y1, double x2, double y2);
	public void fillRect(double x1, double y1, double x2, double y2);
	public void fillTexturedRect(Texture tex, double x1, double y1, double x2, double y2, float s1, float t1, float s2, float t2);
}
