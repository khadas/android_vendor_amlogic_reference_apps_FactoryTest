package cn.com.factorytest.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.view.KeyEvent;
import android.view.MotionEvent;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class TouchSurfaceView extends GLSurfaceView {
  private final float TOUCH_SCALE_FACTOR = 0.5625F;
  private float mPreviousX;
  private float mPreviousY;
  private float mPreviousZ;
  private CubeRenderer mRenderer = new CubeRenderer();

  public TouchSurfaceView(Context paramContext) {
    super(paramContext);
    setRenderer(this.mRenderer);
    setRenderMode(0);
  }

  public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent) {
    if (paramInt == 82) {
      this.mRenderer.mAngleZ = 0.0F;
      mRenderer.mAngleY = 0.0F;
      mRenderer.mAngleX = 0.0F;
    }
    return super.onKeyDown(paramInt, paramKeyEvent);
  }

  public boolean onTouchEvent(MotionEvent paramMotionEvent) {
    this.mRenderer.mAngleZ = 0.0F;
    mRenderer.mAngleY = 0.0F;
    mRenderer.mAngleX = 0.0F;
    return true;
  }

  public void updateGyro(float paramFloat1, float paramFloat2,
                         float paramFloat3) {
    float f1 = paramFloat1 - this.mPreviousX;
    float f2 = paramFloat2 - this.mPreviousY;
    float f3 = paramFloat3 - this.mPreviousZ;
    CubeRenderer localCubeRenderer1 = this.mRenderer;
    localCubeRenderer1.mAngleX += f1 * 0.5625F;
    CubeRenderer localCubeRenderer2 = this.mRenderer;
    localCubeRenderer2.mAngleY += f2 * 0.5625F;
    CubeRenderer localCubeRenderer3 = this.mRenderer;
    localCubeRenderer3.mAngleZ += f3 * 0.5625F;
    requestRender();
    this.mPreviousX = paramFloat1;
    this.mPreviousY = paramFloat2;
    this.mPreviousZ = paramFloat3;
  }

  private class CubeRenderer implements GLSurfaceView.Renderer {
    public float mAngleX;
    public float mAngleY;
    public float mAngleZ;
    private Cube mCube = new Cube();

    public CubeRenderer() {}

    public void onDrawFrame(GL10 gl) {
      gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
      gl.glMatrixMode(GL10.GL_MODELVIEW);
      gl.glLoadIdentity();
      gl.glTranslatef(0.0F, 0.0F, -6.0F);
      gl.glRotatef(this.mAngleY, 1.0F, 0.0F, 0.0F);
      gl.glRotatef(this.mAngleX, 0.0F, 1.0F, 0.0F);
      gl.glRotatef(this.mAngleZ, 0.0F, 0.0F, 1.0F);
      gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
      gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
      this.mCube.draw(gl);
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) {
      gl.glViewport(0, 0, width, height);

      /*
       * Set our projection matrix. This doesn't have to be done
       * each time we draw, but usually a new projection needs to
       * be set when the viewport is resized.
       */

      float ratio = (float)width / height;
      gl.glMatrixMode(GL10.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glFrustumf(-ratio, ratio, -1, 1, 1, 10);
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig paramEGLConfig) {
      /*
       * By default, OpenGL enables features that improve quality
       * but reduce performance. One might want to tweak that
       * especially on software renderer.
       */
      gl.glDisable(GL10.GL_DITHER);

      /*
       * Some one-time OpenGL initialization can be made here
       * probably based on features of this particular context
       */
      gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

      gl.glClearColor(1, 1, 1, 1);
      gl.glEnable(GL10.GL_CULL_FACE);
      gl.glShadeModel(GL10.GL_SMOOTH);
      gl.glEnable(GL10.GL_DEPTH_TEST);
    }
  }
}
