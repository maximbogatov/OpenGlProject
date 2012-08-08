package com.bogatov.openglproj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import com.bogatov.openglproj.GridViewRenderer.IViewDataHandler;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;

public class TextureView extends GLSurfaceView implements IViewDataHandler {
    
    private int mColumnCount = 4;

    public TextureView(Context context){
        this(context, null);
    }
    
    public TextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
     // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);
        
        setEGLConfigChooser(8,8,8,8,16,0);
        
//        GridViewRenderer renderer = new GridViewRenderer(context, this);
        SimpleRenderer renderer = new SimpleRenderer(context);
//        renderer.
        setRenderer(renderer);
//        setRenderer(new TextureViewRenderer());
        
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        
        // Set the Renderer for drawing on the GLSurfaceView
//        setRenderer(new TextureViewRenderer());
    }
    
    @Override
    public int getColumnCount() {
        return mColumnCount;
    }
    
    private class TextureViewRenderer implements GLSurfaceView.Renderer {
        
        private int mProgramHandle;
        private int maPositionHandle;
        private int maTextureCoordinateHandle;
        private int muMVPMatrixHandle;
        private int muMVMatrixHandle;
        private int muTextureHandle;
        private int muLightPosHandle;
        private int maColorHandle;
        private int maNormalHandle;
        
        private int mTextureDataHandle;
        
        private float[] mMVPMatrix = new float[16];
        private float[] mViewMatrix = new float[16];
        private float[] mModelMatrix = new float[16];
        private float[] mProjMatrix = new float[16];
        
        private FloatBuffer mPositionsData;
        private FloatBuffer mTextureCoordinatesData;
        private FloatBuffer mColorsData;
        private FloatBuffer mNormalsData;
        
        public void onSurfaceCreated(GL10 unused, EGLConfig config) {
            // Set the background frame color
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            
            // Use culling to remove back faces.
            GLES20.glEnable(GLES20.GL_CULL_FACE);
            // Enable depth testing
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            // Enable texture mapping
            GLES20.glEnable(GLES20.GL_TEXTURE_2D);
            
            // initialize the triangle vertex array
            initShapes();
            
            // Position the eye in front of the origin.
            final float eyeX = 0.0f;
            final float eyeY = 0.0f;
            final float eyeZ = -0.5f;

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
            
            String vertexShaderCode = RawResourceReader.readTextFileFromRawResource(getContext(), R.raw.per_pixel_vertex_shader);
            String fragmentShaderCode = RawResourceReader.readTextFileFromRawResource(getContext(), R.raw.per_pixel_fragment_shader);
            
            int vertexShader = ShaderUtils.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader = ShaderUtils.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
            
            mProgramHandle = ShaderUtils.createAndLinkProgram(vertexShader, fragmentShader, 
                    new String[] {"a_Position",  "a_Color", "a_Normal", "a_TexCoordinate"});
            
            mTextureDataHandle = TextureUtils.loadTexture(getContext(), R.drawable.ic_launcher);
        }
        
        public void onSurfaceChanged(GL10 unused, int width, int height) {
            // Set the OpenGL viewport to the same size as the surface.
            GLES20.glViewport(0, 0, width, height);
            
            float ratio = (float) width / height;
            
            // this projection matrix is applied to object coodinates
            // in the onDrawFrame() method
            Matrix.frustumM(mProjMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        }
        
        public void onDrawFrame(GL10 unused) {
            // Redraw background color
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            
            // Add program to OpenGL environment
            GLES20.glUseProgram(mProgramHandle);
            
            // Set program handles for cube drawing.
            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
            muMVMatrixHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_MVMatrix"); 
            muTextureHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_Texture");
            maPositionHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Position");
            maTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_TexCoordinate");
            muLightPosHandle = GLES20.glGetUniformLocation(mProgramHandle, "u_LightPos");
            maColorHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Color");
            maNormalHandle = GLES20.glGetAttribLocation(mProgramHandle, "a_Normal"); 
            
            
            // Set the active texture unit to texture unit 0.
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            
            // Bind the texture to this unit.
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureDataHandle);
            
            // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
            GLES20.glUniform1i(muTextureHandle, 0);       
            
            drawTexture();
        }
        
        private void drawTexture() {
            // Pass in the position information
            mPositionsData.position(0);     
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false, 0, mPositionsData);        
            GLES20.glEnableVertexAttribArray(maPositionHandle);        
            
            // Pass in the texture coordinate information
            mTextureCoordinatesData.position(0);
            GLES20.glVertexAttribPointer(maTextureCoordinateHandle, 2, GLES20.GL_FLOAT, false, 0, mTextureCoordinatesData);
            GLES20.glEnableVertexAttribArray(maTextureCoordinateHandle);
            
            // This multiplies the view matrix by the model matrix, and stores the result in the MVP matrix
            // (which currently contains model * view).
            Matrix.multiplyMM(mMVPMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);   
            
            // Pass in the modelview matrix.
            GLES20.glUniformMatrix4fv(muMVMatrixHandle, 1, false, mMVPMatrix, 0);                
            
            // This multiplies the modelview matrix by the projection matrix, and stores the result in the MVP matrix
            // (which now contains model * view * projection).
            Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mMVPMatrix, 0);

            // Pass in the combined matrix.
            GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            
            // Draw the cube.
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);      
        }
        
        private void initShapes() {
            
            float figureCoordsData[] = {
                // X, Y, Z
                -0.5f,  0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                 0.5f,  0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                 0.5f, -0.5f, 0.0f,
                 0.5f,  0.5f, 0.0f
            }; 
            
            float textureCoords[] = {
                    0.0f, 0.0f,                
                    0.0f, 1.0f,
                    1.0f, 0.0f,
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    1.0f, 0.0f   
            };
            
            float colorsData[] = {
                // R, G, B, A
                1.0f, 0.0f, 0.0f, 1.0f,             
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f,             
                1.0f, 0.0f, 0.0f, 1.0f,
                1.0f, 0.0f, 0.0f, 1.0f
            }; 
            
            float normalsData[] = {
                // X, Y, Z
                0.0f, 0.0f, 1.0f,               
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,               
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f
            };
            
            mPositionsData = ByteBuffer.allocateDirect(figureCoordsData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();                            
            mPositionsData.put(figureCoordsData).position(0);       
            
            mTextureCoordinatesData = ByteBuffer.allocateDirect(textureCoords.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();                            
            mTextureCoordinatesData.put(textureCoords).position(0); 
            
            mColorsData = ByteBuffer.allocateDirect(colorsData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();                            
            mColorsData.put(colorsData).position(0); 
            
            mNormalsData = ByteBuffer.allocateDirect(normalsData.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();                            
            mNormalsData.put(normalsData).position(0); 
        }
    }
}
