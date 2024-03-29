package com.bogatov.openglproj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

public class GridViewRenderer implements GLSurfaceView.Renderer {   
    
    private static final String TAG = GridViewRenderer.class.getSimpleName();
    
    private static final int DEFAULT_COLUMN_COUNT = 3;
    
    private static final int DEFAULT_OFFSET_PIX = 0;
    
    private final Context mActivityContext;

    /**
     * Store the model matrix. This matrix is used to move models from object space (where each model can be thought
     * of being located at the center of the universe) to world space.
     */
    private float[] mModelMatrix = new float[16];

    /**
     * Store the view matrix. This can be thought of as our camera. This matrix transforms world space to eye space;
     * it positions things relative to our eye.
     */
    private float[] mViewMatrix = new float[16];

    /** Store the projection matrix. This is used to project the scene onto a 2D viewport. */
    private float[] mProjectionMatrix = new float[16];

    /** Allocate storage for the final combined matrix. This will be passed into the shader program. */
    private float[] mMVPMatrix = new float[16];

    /** Store our model data in a float buffer. */
    private FloatBuffer mCubePositions;
    private FloatBuffer mCubeTextureCoordinates;
    
    private FloatBuffer mLeftVertexBuffer;
    private ShortBuffer mLeftDrawBuffer;
    private FloatBuffer mLeftTextureCoordinates;

    /** This will be used to pass in the transformation matrix. */
    private int mMVPMatrixHandle;

    /** This will be used to pass in the modelview matrix. */
    private int mMVMatrixHandle;

    /** This will be used to pass in the texture. */
    private int mTextureUniformHandle;

    /** This will be used to pass in model position information. */
    private int mPositionHandle;

    /** This will be used to pass in model texture coordinate information. */
    private int mTextureCoordinateHandle;

    /** How many bytes per float. */
    private final int mBytesPerFloat = 4;   

    /** Size of the position data in elements. */
    private final int mPositionDataSize = 3;    

    /** Size of the texture coordinate data in elements. */
    private final int mTextureCoordinateDataSize = 2;

    /** This is a handle to our cube shading program. */
    private int mProgramHandle;

    /** This is a handle to our texture data. */
    private int mTextureDataHandle;

    private IViewDataHandler mViewHandler;
    
    private int mWidth;
    private int mHeight;
    private float mRatio;
    
    private int mColumnCount = DEFAULT_COLUMN_COUNT;
    private int mRowCount = mColumnCount;
    
    private int mCellSize;
    private int mOffset = DEFAULT_OFFSET_PIX;
    
    public GridViewRenderer(final Context context, IViewDataHandler handler) {   
        mActivityContext = context;
        mViewHandler = handler;
    }
    
    public void setColumntCount(int columns) {
        mColumnCount = columns;
    }

    protected String getVertexShader() {
        return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_vertex_shader);
    }

    protected String getFragmentShader() {
        return RawResourceReader.readTextFileFromRawResource(mActivityContext, R.raw.per_pixel_fragment_shader);
    }

    @Override
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        // Set the background clear color to white.
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);

        // Use culling to remove back faces.
//        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // Enable depth testing
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Enable texture mapping
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        
        // Position the eye in front of the origin.
        final float eyeX = 0.0f;
        final float eyeY = 0.0f;
        final float eyeZ = (-1.0f + DEFAULT_COLUMN_COUNT);

        // We are looking toward the distance
        final float lookX = 0.0f;
        final float lookY = 0.0f;
        final float lookZ = -5.0f;

        // Set our up vector. This is where our head would be pointing were we holding the camera.
        final float upX = 0.0f;
        final float upY = 1.0f;
        final float upZ = 0.0f;

        // Set the view matrix. This matrix can be said to represent the camera position.
        // NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination of a model and
        // view matrix. In OpenGL 2, we can keep track of these matrices separately if we choose.
        Matrix.setLookAtM(mViewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

        final String vertexShader = getVertexShader();          
        final String fragmentShader = getFragmentShader();          

        final int vertexShaderHandle = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);       
        final int fragmentShaderHandle = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);     

        mProgramHandle = ShaderUtils.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {"a_Position",  "a_TexCoordinate"});                                                                                                   
        
        // Load the texture
        mTextureDataHandle = TextureUtils.loadTexture(mActivityContext, R.drawable.pirate);
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        
     // Set our per-vertex lighting program.
        GLES20.glUseProgram(mProgramHandle);
        
        // Set program handles for cube drawing.
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVPMatrix");
        mMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix"); 
        mTextureUniformHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
        mPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
        mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
    }   

    @Override
    public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        // Set the OpenGL viewport to the same size as the surface.
        GLES20.glViewport(0, 0, width, height);

        // Create a new perspective projection matrix. The height will stay the same
        // while the width will vary as per aspect ratio.
        mWidth = width;
        mHeight = height;
        mRatio = (float) width/height;
        mRowCount = (int) ((float)mColumnCount / mRatio) + 1;
        
        final float left = -mRatio;
        final float right = mRatio;
        final float bottom = -1.0f;
        final float top = 1.0f;
        final float near = 0.999999f;
        final float far = 10.0f;
        
        Matrix.frustumM(mProjectionMatrix, 0, left, right, bottom, top, near, far);
        
        mCellSize = width/mColumnCount - 2*mOffset;
        
        initTextureSize();
    }   
    
    private void initTextureSize() {
     // X, Y, Z
//        final float[] cubePositionData2 =
//        {
//                // In OpenGL counter-clockwise winding is default. This means that when we look at a triangle, 
//                // if the points are counter-clockwise we are looking at the "front". If not we are looking at
//                // the back. OpenGL has an optimization where all back-facing triangles are culled, since they
//                // usually represent the backside of an object and aren't visible anyways.
//
//                // Front face
//                -(1.0f - offsetRatio),  1.0f - offsetRatio,     0.0f,              
//                -(1.0f - offsetRatio),  -(1.0f - offsetRatio),  0.0f,
//                1.0f - offsetRatio,     1.0f - offsetRatio,     0.0f, 
//                -(1.0f - offsetRatio),  -(1.0f - offsetRatio),  0.0f,                 
//                1.0f - offsetRatio,     -(1.0f - offsetRatio),  0.0f,
//                1.0f - offsetRatio,     1.0f - offsetRatio,     0.0f,
//        };  
        
        final float[] cubePositionData =
        {
                -1.0f*mRatio, 1.0f*mRatio,   -1.0f,              
                -1.0f*mRatio, -1.0f*mRatio,  -1.0f,
                1.0f*mRatio,  1.0f*mRatio,   -1.0f, 
                -1.0f*mRatio, -1.0f*mRatio,  -1.0f,                 
                1.0f*mRatio,  -1.0f*mRatio,  -1.0f,
                1.0f*mRatio,  1.0f*mRatio,   -1.0f,
        };
        
        // S, T (or X, Y)
        // Texture coordinate data.
        // Because images have a Y axis pointing downward (values increase as you move down the image) while
        // OpenGL has a Y axis pointing upward, we adjust for that here by flipping the Y axis.
        // What's more is that the texture coordinates are the same for every face.
        final float[] cubeTextureCoordinateData =
        {                                               
                // Front face
                0.0f, 0.0f,                 
                0.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,             
        };
        
        final float[] leftPosition =
        {
                -1.0f, 0.1f,   -1.0f,              
                -1.0f, -0.1f,  -1.0f,
                -0.9f,  -0.1f,  -1.0f,
                -0.9f,  0.1f,   -1.0f, 
        };
        
        short drawOrder[] = { 0, 1, 2, 0, 2, 3 };

        // Initialize the buffers.
        mCubePositions = ByteBuffer.allocateDirect(cubePositionData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();                            
        mCubePositions.put(cubePositionData).position(0);       

        mCubeTextureCoordinates = ByteBuffer.allocateDirect(cubeTextureCoordinateData.length * mBytesPerFloat).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mCubeTextureCoordinates.put(cubeTextureCoordinateData).position(0);
        
        
        mLeftVertexBuffer = ByteBuffer.allocateDirect(leftPosition.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();  
        mLeftVertexBuffer.put(leftPosition).position(0);
        
        mLeftDrawBuffer = ByteBuffer.allocateDirect(drawOrder.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();  
        mLeftDrawBuffer.put(drawOrder).position(0);
    }

    @Override
    public void onDrawFrame(GL10 glUnused) 
    {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);                    
                
        // Set the active texture unit to texture unit 0.
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        
        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
//        GLES20.glUniform1i(mTextureUniformHandle, 0);        
        
        
        for(int rows = 0; rows < mRowCount; rows++) {
            float yTranslate = 0.0f;//(float) (mRowCount)/mRatio + rows*2*mRatio;
            
            for(int cols = 0; cols < mColumnCount; cols++) {
                float xTranslate = (float) (-mColumnCount + cols*2 + 1)*mRatio;
                
                Matrix.setIdentityM(mModelMatrix, 0);
                Matrix.translateM(mModelMatrix, 0, xTranslate, yTranslate, 0.0f);
                
                drawCube(); 
            }
        }
    }               

    /**
     * Draws a cube.
     */         
    private void drawCube()
    {       
        // Pass in the position information
        mCubePositions.position(0);     
        GLES20.glVertexAttribPointer(mPositionHandle, mPositionDataSize, GLES20.GL_FLOAT, false, 0, mCubePositions);        
        GLES20.glEnableVertexAttribArray(mPositionHandle);        
        
        // Pass in the texture coordinate information
        mCubeTextureCoordinates.position(0);
        GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mTextureCoordinateDataSize, GLES20.GL_FLOAT, false, 0, mCubeTextureCoordinates);
        GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
        
        // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
        // (which currently contains model * view).
        Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   
        
        // Pass in the modelview matrix.
        GLES20.glUniformMatrix4fv(mMVMatrixHandle, 1, false, mMVPMatrix, 0);                
        
        // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
        // (which now contains model * view * projection).
        Matrix.multiplyMM(mMVPMatrix, 0, mProjectionMatrix, 0, mMVPMatrix, 0);

        // Pass in the combined matrix.
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mMVPMatrix, 0);
        
        // Draw the cube.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);        
        
    }   
    
    interface IViewDataHandler {
        public int getColumnCount();
    }
}
