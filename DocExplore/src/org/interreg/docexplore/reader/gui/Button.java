/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.reader.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import org.interreg.docexplore.reader.ReaderApp;
import org.interreg.docexplore.reader.gfx.GfxUtils;
import org.interreg.docexplore.reader.gfx.Texture;
import org.interreg.docexplore.util.ImageUtils;
import org.interreg.docexplore.util.Pair;
import org.lwjgl.input.Mouse;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;

public class Button extends Widget implements GuiEvent.Source
{
	ReaderApp app;
	Texture texture;
	public boolean holdable = false, held = false;
	
	public Button(ReaderApp app, String name) throws Exception
	{
		this.app = app;
		final BufferedImage image = ImageUtils.read(Thread.currentThread().getContextClassLoader().getResource(Button.class.getPackage().getName().replace('.', '/')+"/"+name));
		this.w = image.getWidth()*.66f;
		this.h = image.getHeight()*.66f;
		Graphics2D g = image.createGraphics();
		g.setStroke(GuiLayer.defaultStroke);
		g.setColor(Color.white);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawRoundRect(2, 2, image.getWidth()-4, image.getHeight()-4, 20, 20);
		app.submitRenderTaskAndWait(new Runnable() {public void run() {texture = new Texture(image, false);}});
	}
	
	public void doClick() {clicked(x+.5f*w, y+.5f*h);}
	public void clicked(float x, float y)
	{
		held = false;
		ActionEvent event = new ActionEvent(new Pair<Object, Object>(this, null), 0, null);
		synchronized (currentMonitor)
		{
			for (ActionListener listener : listeners)
				listener.actionPerformed(event);
			currentMonitor.notifyAll();
		}
	}
	public void held(float x, float y)
	{
		held = true;
	}
	public void dropped(float x, float y)
	{
		held = false;
	}
	
	public float x = -1, y = -1, w = 0, h = 0;
	public float destx = -1, desty = -1;
	float [] color = new float [] {GuiLayer.defaultColor[0], GuiLayer.defaultColor[1], GuiLayer.defaultColor[2]}, 
		destColor = new float [] {GuiLayer.defaultColor[0], GuiLayer.defaultColor[1], GuiLayer.defaultColor[2]};
	public void setColor(float r, float g, float b) {color[0] = r; color[0] = g; color[0] = b;}
	public void setColor(float [] to) {color[0] = to[0]; color[0] = to[1]; color[0] = to[2]; destColor[0] = to[0]; destColor[1] = to[1]; destColor[2] = to[2];}
	public void setDestColor(float [] to) {destColor[0] = to[0]; destColor[1] = to[1]; destColor[2] = to[2];}
	public void setPosition(float x, float y) {this.x = x; this.y = y; this.destx = x; this.desty = y;}
	public void setDestPosition(float x, float y) {this.destx = x; this.desty = y;}
	public float getX() {return active ? x : -1;}
	public float getY() {return active ? y : -1;}
	public float getWidth() {return active ? w : 0;}
	public float getHeight() {return active ? h : 0;}
	
	public void updateWidget()
	{
		float k = .1f;
		x += k*(destx-x);
		y += k*(desty-y);
		color[0] += k*(destColor[0]-color[0]);
		color[1] += k*(destColor[1]-color[1]);
		color[2] += k*(destColor[2]-color[2]);
		
		int mx = Mouse.getX(), my = Gdx.graphics.getHeight()-Mouse.getY();
		if (mx < x || mx > x+w || my < y || my > y+h)
			held = false;
		if (holdable && held)
		{
			ActionEvent event = new ActionEvent(new Pair<Object, Object>(this, null), 0, null);
			for (ActionListener listener : listeners)
				listener.actionPerformed(event);
			try {synchronized (currentMonitor) {currentMonitor.notifyAll();}}
			catch (Exception e) {}
		}
	}

	public void renderWidget()
	{
		GL10 gl = Gdx.gl10;
		
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_CULL_FACE);
		gl.glDisable(GL10.GL_STENCIL_TEST);
		
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glColor4f(color[0], color[1], color[2], .75f*alpha);
		texture.bind();
		GfxUtils.fillQuad(x, y, 0, 0, x+w, y+h, 1, 1);
		
		gl.glEnable(GL10.GL_DEPTH_TEST);
		gl.glDisable(GL10.GL_TEXTURE_2D);
	}

	List<ActionListener> listeners = new LinkedList<ActionListener>();
	public void addActionListener(ActionListener listener) {listeners.add(listener);}
	public void removeActionListener(ActionListener listener) {listeners.remove(listener);}
	Object instanceMonitor = new Object();
	Object currentMonitor = instanceMonitor;
	public Object getDefaultMonitor() {return instanceMonitor;}
	public Object getCurrentMonitor() {return currentMonitor;}
	public void setCurrentMonitor(Object o) {currentMonitor = o;}
	public Object waitForEvent() throws InterruptedException {synchronized (currentMonitor) {currentMonitor.wait();} return null;}
}
