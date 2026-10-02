package com.zero.measure.ar.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * Renders detected ARCore planes using a subtle translucent visual overlay.
 */
class PlaneRenderer {

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_MvpMatrix;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MvpMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        // Horizontal plane: clean translucent cyan tint (Material 3 style)
        private val HORIZONTAL_COLOR = floatArrayOf(0.12f, 0.53f, 0.90f, 0.25f)
        // Vertical plane: subtle amber/yellow tint
        private val VERTICAL_COLOR = floatArrayOf(0.95f, 0.65f, 0.15f, 0.25f)
    }

    private var program: Int = 0
    private var mvpMatrixUniform: Int = 0
    private var colorUniform: Int = 0
    private var positionAttrib: Int = 0

    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    fun createOnGlThread() {
        val vertexShader = ShaderUtil.loadGLShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadGLShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        mvpMatrixUniform = GLES20.glGetUniformLocation(program, "u_MvpMatrix")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
    }

    fun draw(planes: Collection<Plane>, viewMatrix: FloatArray, projectionMatrix: FloatArray) {
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDepthMask(false)

        GLES20.glUseProgram(program)
        GLES20.glEnableVertexAttribArray(positionAttrib)

        for (plane in planes) {
            if (plane.trackingState != TrackingState.TRACKING || plane.subsumedBy != null) {
                continue
            }

            val polygon = plane.polygon ?: continue
            val numPoints = polygon.limit() / 2
            if (numPoints < 3) continue

            // Build 3D vertex buffer from 2D plane polygon (x, 0, z) + center fan
            val vertexCount = numPoints + 2
            val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(vertexCount * 3 * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()

            // Center vertex
            vertexBuffer.put(0.0f)
            vertexBuffer.put(0.0f)
            vertexBuffer.put(0.0f)

            polygon.rewind()
            while (polygon.hasRemaining()) {
                val px = polygon.get()
                val pz = polygon.get()
                vertexBuffer.put(px)
                vertexBuffer.put(0.0f)
                vertexBuffer.put(pz)
            }
            // Close the fan with first point
            polygon.rewind()
            vertexBuffer.put(polygon.get())
            vertexBuffer.put(0.0f)
            vertexBuffer.put(polygon.get())

            vertexBuffer.position(0)

            plane.centerPose.toMatrix(modelMatrix, 0)
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, modelViewProjectionMatrix, 0)

            val color = if (plane.type == Plane.Type.VERTICAL) VERTICAL_COLOR else HORIZONTAL_COLOR
            GLES20.glUniform4fv(colorUniform, 1, color, 0)

            GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCount)
        }

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDepthMask(true)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
