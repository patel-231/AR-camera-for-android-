package com.zero.measure.ar.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.zero.measure.model.Point3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders camera-oriented 3D circular pins at Point A, Point B, or hit preview locations.
 */
class PointMarkerRenderer {

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_Mvp;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_Mvp * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        private const val NUM_SEGMENTS = 28
    }

    private var program: Int = 0
    private var mvpUniform: Int = 0
    private var colorUniform: Int = 0
    private var positionAttrib: Int = 0

    private val discVertices: FloatBuffer
    private val ringVertices: FloatBuffer

    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val viewProjMatrix = FloatArray(16)
    private val invViewMatrix = FloatArray(16)

    init {
        // Build 2D circle mesh in XY plane, center at (0,0)
        // Solid disc
        val discCoords = FloatArray((NUM_SEGMENTS + 2) * 3)
        discCoords[0] = 0f
        discCoords[1] = 0f
        discCoords[2] = 0f

        val radius = 0.012f // 1.2 cm radius
        for (i in 0..NUM_SEGMENTS) {
            val theta = (i * 2.0 * Math.PI / NUM_SEGMENTS).toFloat()
            val idx = (i + 1) * 3
            discCoords[idx] = radius * cos(theta)
            discCoords[idx + 1] = radius * sin(theta)
            discCoords[idx + 2] = 0f
        }

        discVertices = ByteBuffer.allocateDirect(discCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(discCoords)
                position(0)
            }

        // Outer ring
        val ringCoords = FloatArray(NUM_SEGMENTS * 3)
        val outerRadius = 0.016f // 1.6 cm radius
        for (i in 0 until NUM_SEGMENTS) {
            val theta = (i * 2.0 * Math.PI / NUM_SEGMENTS).toFloat()
            val idx = i * 3
            ringCoords[idx] = outerRadius * cos(theta)
            ringCoords[idx + 1] = outerRadius * sin(theta)
            ringCoords[idx + 2] = 0f
        }

        ringVertices = ByteBuffer.allocateDirect(ringCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(ringCoords)
                position(0)
            }
    }

    fun createOnGlThread() {
        val vertexShader = ShaderUtil.loadGLShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadGLShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        mvpUniform = GLES20.glGetUniformLocation(program, "u_Mvp")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
    }

    /**
     * Renders a 3D marker oriented toward the camera at point's world coordinates.
     */
    fun drawPointMarker(
        point: Point3D,
        viewMatrix: FloatArray,
        projMatrix: FloatArray,
        ringColor: FloatArray = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f),
        centerColor: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    ) {
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Billboard orientation: extract camera rotation so marker faces camera
        Matrix.invertM(invViewMatrix, 0, viewMatrix, 0)

        // Model matrix with camera rotation and point translation
        Matrix.setIdentityM(modelMatrix, 0)
        // Copy rotation from inverse view matrix
        modelMatrix[0] = invViewMatrix[0]
        modelMatrix[1] = invViewMatrix[1]
        modelMatrix[2] = invViewMatrix[2]
        modelMatrix[4] = invViewMatrix[4]
        modelMatrix[5] = invViewMatrix[5]
        modelMatrix[6] = invViewMatrix[6]
        modelMatrix[8] = invViewMatrix[8]
        modelMatrix[9] = invViewMatrix[9]
        modelMatrix[10] = invViewMatrix[10]

        // Translation
        modelMatrix[12] = point.x
        modelMatrix[13] = point.y
        modelMatrix[14] = point.z

        Matrix.multiplyMM(mvpMatrix, 0, viewProjMatrix, 0, modelMatrix, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvpMatrix, 0)

        // Draw inner disc
        GLES20.glUniform4fv(colorUniform, 1, centerColor, 0)
        discVertices.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, discVertices)
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, NUM_SEGMENTS + 2)

        // Draw outer ring
        GLES20.glUniform4fv(colorUniform, 1, ringColor, 0)
        ringVertices.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, ringVertices)
        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, NUM_SEGMENTS)

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
