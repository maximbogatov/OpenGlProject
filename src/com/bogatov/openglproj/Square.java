package com.bogatov.openglproj;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class Square {

    private FloatBuffer mVertexBuffer;
    private ShortBuffer mDrawListBuffer;
    
    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 3;
    private static float squareCoords[] = { -0.5f,  0.5f, -1.0f,   // top left
                                    -0.5f, -0.5f, -1.0f,   // bottom left
                                     0.5f, -0.5f, -1.0f,   // bottom right
                                     0.5f,  0.5f, -1.0f }; // top right

    private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices
    private float color[] = { 0.63671875f, 0.76953125f, 0.22265625f, 1.0f };

    private static final int vertexStride = COORDS_PER_VERTEX * 4;
    private static final int vertexCount = 4;

    public Square() {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());
        mVertexBuffer = bb.asFloatBuffer();
        mVertexBuffer.put(squareCoords);
        mVertexBuffer.position(0);

        // initialize byte buffer for the draw list
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        mDrawListBuffer = dlb.asShortBuffer();
        mDrawListBuffer.put(drawOrder);
        mDrawListBuffer.position(0);
    }

    public void draw(int programHandler, int colorHandler, int positionHandler) {
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(positionHandler);

        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(positionHandler, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, mVertexBuffer);

        // Set color for drawing the triangle
        GLES20.glUniform4fv(colorHandler, 1, color, 0);

        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(positionHandler);
    }
}