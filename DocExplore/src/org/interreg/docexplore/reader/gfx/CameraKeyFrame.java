/**
Copyright LITIS/EDA 2014
contact@docexplore.eu

This software is a computer program whose purpose is to manage and display interactive digital books.

This software is governed by the CeCILL license under French law and abiding by the rules of distribution of free software.  You can  use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".

As a counterpart to the access to the source code and  rights to copy, modify and redistribute granted by the license, users are provided only with a limited warranty  and the software's author,  the holder of the economic rights,  and the successive licensors  have only  limited liability.

In this respect, the user's attention is drawn to the risks associated with loading,  using,  modifying and/or developing or reproducing the software by the user in light of its specific status of free software, that may mean  that it is complicated to manipulate,  and  that  also therefore means  that it is reserved for developers  and  experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the software's suitability as regards their requirements in conditions enabling the security of their systems and/or data to be ensured and,  more generally, to use and operate it in the same conditions as regards security.

The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you accept its terms.
 */
package org.interreg.docexplore.reader.gfx;

import java.util.Arrays;

import org.interreg.docexplore.reader.book.BookModel;
import org.interreg.docexplore.reader.book.BookPageStack;
import org.interreg.docexplore.reader.book.ROISpecification;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.math.Vector3;

public class CameraKeyFrame
{
	public final float [] pos, dir, up;
	public float fov;

	public CameraKeyFrame(float [] pos, float [] dir, float [] up, float fov)
	{
		this.pos = pos;
		this.dir = dir;
		this.up = up;
		this.fov = fov;
	}
	public CameraKeyFrame(CameraKeyFrame frame)
	{
		this(Arrays.copyOf(frame.pos, 3), Arrays.copyOf(frame.dir, 3), Arrays.copyOf(frame.up, 3), frame.fov);
	}
	public CameraKeyFrame()
	{
		this.pos = new float [] {0, 0, 0};
		this.dir = new float [] {0, 0, 1};
		this.up = new float [] {0, 0, 0};
		this.fov = 90;
	}
	
	float [] buf1 = {0, 0, 0}, buf2 = {0, 0, 0};
	public CameraKeyFrame setup(float ex, float ey, float ez, float tx, float ty, float tz, float ux, float uy, float uz, float fov)
	{
		Math3D.set(pos, ex, ey, ez);
		Math3D.set(dir, tx, ty, tz);
		Math3D.diff(pos, dir, dir);
		
		Math3D.set(up, ux, uy, uz);
		Math3D.crossProduct(up, dir, buf1);
		Math3D.crossProduct(dir, buf1, up);
		Math3D.normalize(dir, dir);
		Math3D.normalize(up, up);
		this.fov = fov;
		
		return this;
	}
	
	float [] tl = {0, 0, 0}, br = {0, 0, 0}, middle = {0, 0, 0};
	/**
	 * Configures this frame to properly view a given region.
	 * @param model
	 * @param region
	 * @param left Left page
	 */
	public void setup(BookModel model, ROISpecification region, boolean left)
	{
		BookPageStack stack = left ? model.leftStack : model.rightStack;
		stack.fromPage(region.shape.minx, 1-region.shape.miny, tl);
		stack.fromPage(region.shape.maxx, 1-region.shape.maxy, br);
		
		float margin = .15f;
		float xm = margin*(br[0]-tl[0]);
		tl[0] -=  xm; br[0] += xm;
		float ym = margin*(tl[1]-br[1]);
		tl[1] += ym; br[1] -= ym;
		
		float fov = (float)(.1*Math.PI);
		float aspect = Gdx.graphics.getWidth()*1f/Gdx.graphics.getHeight();
		float viewDistForX = (float)(1.78/aspect*(br[0]-tl[0])/(Math.tan(fov)));
		float viewDistForY = (float)((tl[1]-br[1])/(2*Math.tan(.5f*fov)));
		
		float viewDist = 1.2f*Math.max(viewDistForX, viewDistForY);
		
		float eyex = (float)((br[0]+tl[0])/2+aspect/1.78*viewDist*Math.tan(.5f*fov));
		float midy = (tl[1]+br[1])/2, midz = (tl[2]+br[2])/2;
		float right = (float)(.5*Math.sin(.5f*fov)*viewDist);
		
//		if (viewDist < .5f)
//			viewDist = .5f;
		
		setup(eyex, midy, midz+viewDist, 
			eyex, midy, midz, 
			0, 1, 1, fov);
	}
	
	void attract(Vector3 v, float [] f, float amount)
	{
		v.x += amount*(f[0]-v.x);
		v.y += amount*(f[1]-v.y);
		v.z += amount*(f[2]-v.z);
	}
	
	/**
	 * Attracts a camera to this key frame.
	 * @param camera
	 * @param amount 0 will not affect the camera, 1 will set it to this key frame.
	 */
	public void attract(PerspectiveCamera camera, float amount)
	{
		attract(camera.position, pos, amount);
		attract(camera.direction, dir, amount);
		attract(camera.up, up, amount);
		camera.fieldOfView += amount*(180*fov/Math.PI-camera.fieldOfView);
		camera.normalizeUp();
		camera.update();
	}
}
