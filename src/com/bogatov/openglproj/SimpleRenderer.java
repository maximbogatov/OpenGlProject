package com.bogatov.openglproj;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class SimpleRenderer implements GLSurfaceView.Renderer {   
    
    private float[] mModelMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private float[] mMVPMatrix = new float[16];
    
    private Context mContext;
    
    private int mTextureProgramHandle;
    private int mTextureDataHandle;
    private int mMVPMatrixHandle;
    private int mMVMatrixHandle;
    private int mTextureUniformHandle;
    private int mPositionHandle;
    private int mTextureCoordinateHandle;
    
    private int mColorProgramHandler;
    private int mColorPositionHandler;
    private int mColorColorHandler;

    /** Store our model data in a float buffer. */
    private Square mSquare;

    public SimpleRenderer(final Context context) {  
        mContext = context;
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST | GLES20.GL_TEXTURE_2D | GLES20.GL_CULL_FACE);
        
        ininViewMatrix();
        initShaders();

        mSquare = new Square();
    }   
    
    private void ininViewMatrix() {
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = 0.0f;

        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);
    }
    
    private void initShaders() {
        initTextureShaders(); 
        initColorShaders();
    }
    
    private void initTextureShaders() {
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.per_pixel_vertex_shader);      
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.per_pixel_fragment_shader);
        
        final int vertexShaderHandle = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);       
        final int fragmentShaderHandle = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);     
        
        mTextureProgramHandle = ShaderUtils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {"a_Position",  "a_TexCoordinate"});
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        
        // Set our per-vertex lighting program.
        GLES20.glUseProgram(mTextureProgramHandle);
        
        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mTextureProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mTextureProgramHandle, "u_MVMatrix"); 
        mTextureUniformHandle = GLES20.glGetUniformLocation(mTextureProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mTextureProgramHandle, "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mTextureProgramHandle, "a_TexCoordinate");
    }
    
    private void initColorShaders() {
        final String vertexShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.color_vertex_shader);    
        final String fragmentShader = RawResourceReader.readTextFileFromRawResource(mContext, R.raw.color_fragment_shader); 
        
        final int vertexShaderHandle = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);       
        final int fragmentShaderHandle = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);     
        
        mColorProgramHandler = ShaderUtils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {"aPosition"});
        
        GLES20.glUseProgram(mColorProgramHandler);
        
        mColorPositionHandler = GLES20.glGetAttribLocation(mTextureProgramHandle, "aPosition");
        mColorColorHandler = GLES20.glGetUniformLocation(mTextureProgramHandle, "uColor");
    }
    
    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width/height;
        
        final float left = -ratio;
        final float right = ratio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 0.999999f;
        final float far = 10.0f;
        
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
    }   
    
    @Override
    public void onDrawFrame(GL10 glUnused) 
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);   
        
        Matrix.setIdentityM(mModelMatrix, 0);
        
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                
        
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        
        GLES20.glUseProgram(mColorProgramHandler);
        mSquare.draw(mColorProgramHandler, mColorColorHandler, mColorPositionHandler);    
    }               
}
