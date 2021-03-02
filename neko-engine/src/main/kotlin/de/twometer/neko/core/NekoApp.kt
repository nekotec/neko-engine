package de.twometer.neko.core

import de.twometer.neko.Neko
import de.twometer.neko.events.Events
import de.twometer.neko.scene.Scene
import de.twometer.neko.util.CrashHandler
import de.twometer.neko.util.Timer
import mu.KotlinLogging
import org.lwjgl.opengl.GL11.*

private val logger = KotlinLogging.logger {}

open class NekoApp(config: AppConfig) {

    companion object {
        var the: NekoApp? = null
    }

    val window = Window(config)
    val timer = Timer(config.timerSpeed)
    val scene = Scene()

    fun run() {
        if (the != null)
            error("Only one NekoApp instance is allowed")
        else the = this

        logger.info { "Starting Neko Engine v${Neko.VERSION}" }
        CrashHandler.register()
        Events.setup()

        onPreInit()

        window.create()

        val version = glGetString(GL_VERSION)
        val vendor = glGetString(GL_VENDOR)
        val os = System.getProperty("os.name>")
        logger.info { "Detected OpenGL $version ($vendor) on $os" }

        onPostInit()

        while (!window.isCloseRequested()) {
            scene.camera.update()

            onRenderFrame()

            if (timer.elapsed()) {
                onTimerTick()
                timer.reset()
            }

            window.update()
        }

        logger.info { "Shutting down..." }
        onShutdown()
        window.destroy()
    }

    open fun onPreInit() = Unit
    open fun onPostInit() = Unit
    open fun onRenderFrame() = Unit
    open fun onTimerTick() = Unit
    open fun onShutdown() = Unit

}