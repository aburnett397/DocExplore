/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.management.process.align;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;


public class BackgroundRemoval
{
	static int red(int rgb) {return (rgb >> 16) & 0x000000FF;}
	static int green(int rgb) {return (rgb >> 8 ) & 0x000000FF;}
	static int blue(int rgb) {return (rgb) & 0x000000FF;}
	static int rgb(int r, int g, int b) {return (r << 16)+(g << 8)+b;}
	
	public static int diff(int rgb1, int rgb2)
	{
		int rd = Math.abs(red(rgb1)-red(rgb2));
		int gd = Math.abs(green(rgb1)-green(rgb2));
		int bd = Math.abs(blue(rgb1)-blue(rgb2));
		
		return Math.max(rd, Math.max(gd, bd));
	}
	
	public static int gradAt(BufferedImage image, int i, int j)
	{
		int mid = image.getRGB(i, j);
		int bottom = j == 0 ? mid : image.getRGB(i, j-1);
		int top = j == image.getHeight()-1 ? mid : image.getRGB(i, j+1);
		int left = i == 0 ? mid : image.getRGB(i-1, j);
		int right = i == image.getWidth()-1 ? mid : image.getRGB(i+1, j);
		
		return Math.max(diff(mid, bottom), Math.max(diff(mid, top), Math.max(diff(mid, left), diff(mid, right))));
	}
	
	public static int [][] buildGrad(BufferedImage input)
	{
		int [][] output = new int [input.getWidth()][input.getHeight()];
		for (int i=0;i<input.getWidth();i++)
			for (int j=0;j<input.getHeight();j++)
				output[i][j] = gradAt(input, i, j);
		return output;
	}
	
	public static int [][] blurGrad(int [][] grad)
	{
		int [][] res = new int [grad.length][grad[0].length];
		for (int i=0;i<grad.length;i++)
			for (int j=0;j<grad[0].length;j++)
		{
			int sum = grad[i][j];
			int nPixels = 1;
			if (i > 0) {sum += grad[i-1][j]; nPixels++;}
			if (i < grad.length-1) {sum += grad[i+1][j]; nPixels++;}
			if (j > 0) {sum += grad[i][j-1]; nPixels++;}
			if (j < grad[0].length-1) {sum += grad[i][j+1]; nPixels++;}
			res[i][j] = sum/nPixels;
		}
		return res;
	}
	
	public static BufferedImage gradWithImage(BufferedImage input, int [][] grad)
	{
		BufferedImage res = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<input.getWidth();i++)
			for (int j=0;j<input.getHeight();j++)
				if (grad[i][j] > 22)
					res.setRGB(i, j, input.getRGB(i, j));
		return res;
	}
	
	public static int backgroundCol(BufferedImage input, int [][] grad)
	{
		int r = 0, g = 0, b = 0;
		int sum = 0;
		for (int i=0;i<grad.length;i++)
			for (int j=0;j<grad[0].length;j++)
				if (grad[i][j] < 7)
		{
			r += red(input.getRGB(i, j));
			g += green(input.getRGB(i, j));
			b += blue(input.getRGB(i, j));
			sum ++;
		}
		r /= sum; g /= sum; b /= sum;
		return rgb(r, g, b);
	}
	
	public static BufferedImage removeBackground(BufferedImage input, int backCol)
	{
		BufferedImage res = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<input.getWidth();i++)
			for (int j=0;j<input.getHeight();j++)
				if (diff(backCol, input.getRGB(i, j)) < 70)
					res.setRGB(i, j, 0);
				else res.setRGB(i, j, input.getRGB(i, j));
		return res;
	}
	public static BufferedImage buildBackground(BufferedImage input, int backCol)
	{
		BufferedImage res = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<input.getWidth();i++)
			for (int j=0;j<input.getHeight();j++)
				if (diff(backCol, input.getRGB(i, j)) < 70)
					res.setRGB(i, j, input.getRGB(i, j));
				else res.setRGB(i, j, backCol);
		return res;
	}
	
	public static BufferedImage gradToImage(int [][] grad)
	{
		BufferedImage res = new BufferedImage(grad.length, grad[0].length, BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<grad.length;i++)
			for (int j=0;j<grad[0].length;j++)
				res.setRGB(i, j, rgb(grad[i][j], grad[i][j], grad[i][j]));
		return res;
	}
	
	public static int lineScore(int [][] grad, int line)
	{
		int sum = 0;
		for (int i=0;i<grad.length;i++)
			sum += grad[i][line];
		return sum;
	}
	
	public static void main(String [] args) throws Exception
	{
		BufferedImage image = ImageIO.read(new File("C:\\Users\\Alex\\Documents\\manuscrits\\sacramentaire\\7004.jpeg"));
		int [][] grad = buildGrad(image);
		grad = blurGrad(grad);
		BufferedImage output = removeBackground(image, backgroundCol(image, grad));
		
		ImageIO.write(output, "PNG", new File("C:\\sci\\out2.png"));
	}
	
}
