package com.zero.measure.ar.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.zero.measure.model.Point3D
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * Renders a bold, camera-facing 3D line ribbon in ARCore world space between two points.
 * Stays physically locked to real-world coordinates as the camera moves.
 */
class WorldLineRenderer {

    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 u_ViewProjection;
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_ViewProjection * a_Position;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        // Confirmed measurement line: High-contrast bright gold/yellow (iPhone Measure style)
        private val COLOR_CONFIRMED = floatArrayOf(1.0f, 0.84f, 0.0f, 1.0f)
        // Live preview line: Vibrant cyan with slight pulse/transparency
        private val COLOR_PREVIEW = floatArrayOf(0.22f, 0.74f, 0.97f, 0.85f)
        // Accent core: Clean white highlight
        private val COLOR_CORE = floatArrayOf(1.0f, 1.0f, 1.0f, 0.95f)
    }

    private var program: Int = 0
    private var viewProjUniform: Int = 0
    private var colorUniform: Int = 0
    private var positionAttrib: Int = 0

    private val viewProjMatrix = FloatArray(16)
    private val invViewMatrix = FloatArray(16)

    fun createOnGlThread() {
        val vertexShader = ShaderUtil.loadGLShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = ShaderUtil.loadGLShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        positionAttrib = GLES20.glGetAttribLocation(program, "a_Position")
        viewProjUniform = GLES20.glGetUniformLocation(program, "u_ViewProjection")
        colorUniform = GLES20.glGetUniformLocation(program, "u_Color")
    }

    /**
     * Draws a 3D line ribbon between p1 and p2.
     *
     * @param p1 Start world point
     * @param p2 End world point
     * @param isPreview If true, styled as temporary live preview; else confirmed solid line
     * @param viewMatrix ARCore camera view matrix
     * @param projMatrix ARCore camera projection matrix
     */
    fun drawLine(
        p1: Point3D,
        p2: Point3D,
        isPreview: Boolean,
        viewMatrix: FloatArray,
        projMatrix: FloatArray
    ) {
        // Calculate MVP
        Matrix.multiplyMM(viewProjMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // Camera position in world space is translation in inverse view matrix
        Matrix.invertM(invViewMatrix, 0, viewMatrix, 0)
        val camX = invViewMatrix[12]
        val camY = invViewMatrix[13]
        val camZ = invViewMatrix[14]

        // Direction vector from p1 to p2
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val dz = p2.z - p1.z
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 0.001f) return // Too short to draw

        // Midpoint to camera vector
        val mx = (p1.x + p2.x) * 0.5f
        val my = (p1.y + p2.y) * 0.5f
        val mz = (p1.z + p2.z) * 0.5f

        val vx = camX - mx
        val vy = camY - my
        val vz = camZ - mz

        // Cross product D x V
        var nx = dy * vz - dz * vy
        var ny = dz * vx - dx * vz
        var nz = dx * vy - dy * vx
        val nLen = sqrt(nx * nx + ny * ny + nz * nz)

        if (nLen < 0.0001f) {
            nx = 0f
            ny = 1f
            nz = 0f
        } else {
            nx /= nLen
            ny /= nLen
            nz /= nLen
        }

        // Half width of ribbon: ~4mm in world space (visible and precise)
        val halfWidth = if (isPreview) 0.0035f else 0.005f
        val ox = nx * halfWidth
        val oy = ny * halfWidth
        val oz = nz * halfWidth

        // 4 vertices of the quad ribbon
        val ribbonCoords = floatArrayOf(
            p1.x + ox, p1.y + oy, p1.z + oz,
            p1.x - ox, p1.y - oy, p1.z - oz,
            p2.x + ox, p2.y + oy, p2.z + oz,
            p2.x - ox, p2.y - oy, p2.z - oz
        )

        val vertexBuffer: FloatBuffer = ByteBuffer.allocateDirect(ribbonCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(ribbonCoords)
                position(0)
            }

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST) // Keep measurement line visible over surfaces

        GLES20.glUseProgram(program)
        GLES20.glUniformMatrix4fv(viewProjUniform, 1, false, viewProjMatrix, 0)

        // Outer ribbon color
        val color = if (isPreview) COLOR_PREVIEW else COLOR_CONFIRMED
        GLES20.glUniform4fv(colorUniform, 1, color, 0)

        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionAttrib)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        // Draw center thin bright spine
        val spineWidth = halfWidth * 0.35f
        val sox = nx * spineWidth
        val soy = ny * spineWidth
        val soz = nz * spineWidth

        val spineCoords = floatArrayOf(
            p1.x + sox, p1.y + soy, p1.z + soz,
            p1.x - sox, p1.y - soy, p1.z - soz,
            p2.x + sox, p2.y + soy, p2.z + soz,
            p2.x - sox, p2.y - soy, p2.z - soz
        )
        val spineBuffer = ByteBuffer.allocateDirect(spineCoords.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(spineCoords)
                position(0)
            }

        GLES20.glUniform4fv(colorUniform, 1, COLOR_CORE, 0)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, spineBuffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionAttrib)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
