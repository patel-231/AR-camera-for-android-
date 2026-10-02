package com.zero.measure.ar.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.google.ar.core.PointCloud

/**
 * Renders ARCore real-time 3D feature points (point cloud) as subtle glowing particles.
 * Provides immediate visual feedback that world tracking and SLAM is actively scanning.
 */
class PointCloudRenderer {

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_Mvp;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_Mvp * vec4(a_Position.xyz, 1.0);
                gl_PointSize = 5.0;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                // Circular point particle
                vec2 coord = gl_PointCoord - vec2(0.5);
                if (length(coord) > 0.5) {
                    discard;
                }
                gl_FragColor = u_Color;
            }
        """

        private val POINT_COLOR = floatArrayOf(0.98f, 0.75f, 0.18f, 0.75f) // Glowing warm gold
    }

    private var program: Int = 0
    private var positionAttrib: Int = 0
    private var mvpUniform: Int = 0
    private var colorUniform: Int = 0

    private val mvpMatrix = FloatArray(16)

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

    fun draw(pointCloud: PointCloud, viewMatrix: FloatArray, projMatrix: FloatArray) {
        val points = pointCloud.points ?: return
        val count = points.limit() / 4
        if (count == 0) return

        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(mvpUniform, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(colorUniform, 1, POINT_COLOR, 0)

        points.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 4, GLES20.GL_FLOAT, false, 16, points)
        GLES20.glEnableVertexAttribArray(positionAttrib)

        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count)

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
