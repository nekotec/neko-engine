import de.twometer.neko.core.AppConfig
import de.twometer.neko.core.NekoApp
import de.twometer.neko.events.KeyPressEvent
import de.twometer.neko.res.AssetManager
import de.twometer.neko.res.CubemapCache
import de.twometer.neko.res.ModelLoader
import de.twometer.neko.res.RawLoader
import de.twometer.neko.scene.Color
import de.twometer.neko.scene.nodes.*
import de.twometer.neko.util.MathF.toRadians
import de.twometer.neko.util.Profiler
import imgui.ImGui
import org.greenrobot.eventbus.Subscribe
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_ALT
import java.util.*

class DemoApp : NekoApp(AppConfig(windowTitle = "Neko Engine Demo")) {

    private lateinit var rin: ModelNode
    private lateinit var sky: Sky

    private val performanceProfile = TreeMap<String, Double>()

    override fun onPreInit() {
        AssetManager.registerPath("./neko-engine/assets")
        AssetManager.registerPath("./neko-demo/assets")
    }

    override fun onPostInit() {
       // Profiler.enable()

        scene.rootNode.attachChild(ModelLoader.load("rin.fbx").also {
            it.transform.translation.set(0.75f, 0f, 0f)
            it.transform.scale.set(0.01, 0.01, 0.01)
            it.playAnimation(it.animations[0])
            rin = it
        })

        scene.rootNode.attachChild(ModelLoader.load("girl.fbx").also {
            it.transform.translation.set(2f, 0f, 0f)
            it.transform.scale.set(0.01, 0.01, 0.01)
            it.playAnimation(it.animations[0])
        })

        scene.rootNode.attachChild(ModelLoader.load("test.fbx"))

        scene.rootNode.attachChild(ModelLoader.load("ground.fbx").also {
            it.transform.rotation.rotateX(toRadians(-90f))
            it.transform.translation.y = -1f
        })

        scene.rootNode.attachChild(ModelLoader.load("demo-run.fbx").also {
            it.transform.translation.set(5f, 0f, 0f)
            it.transform.rotation.rotateY(toRadians(15f))
            it.transform.scale.set(0.01, 0.01, 0.01)
            it.playAnimation(it.animations[0])
        })

        scene.rootNode.attachChild(PointLight().also { it.color = Color(1f, 1f, 1f, 1f) })
        scene.rootNode.attachChild(PointLight().also {
            it.color = Color(0f, 1f, 1f, 1f)
            it.transform.translation.set(2f, 2f, 0f)
        })

        scene.rootNode.attachChild(ModelLoader.load("skeld.obj"))

        val lightPositions = RawLoader.loadLines("lights.txt")
        for (pos in lightPositions) {
            val parts = pos.split("|")
            scene.rootNode.attachChild(PointLight().also {
                it.transform.translation.set(Vector3f(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat()))
                if (parts.size > 3)
                    it.color = Color(parts[3].toFloat(), parts[4].toFloat(), parts[5].toFloat(), 1f)
                else
                    it.color = Color(1f, 1f, 1f, 1f)
            })
        }

        scene.rootNode.detachAll {
            it is Geometry && it.material.name.startsWith("Translucent_Glass_Blue")
        }

        sky = Sky(CubemapCache.get("skybox"))
        scene.rootNode.attachChild(sky)
    }

    override fun onTimerTick() {
        //sky.transform.rotation.rotateY(0.0002f)
        rin.transform.translation.z += 0.025f
    }

    var selectedNode: Node? = null

    override fun onRenderFrame() {
        ImGui.begin("Scenegraph")
        fun drawNode(node: Node) {
            ImGui.pushID(node.id)
            val open = ImGui.treeNode("${node.id} - ${node.javaClass.simpleName} [${node.name}]")
            if (ImGui.isItemClicked()) {
                selectedNode = node
            }
            if (open) {
                node.children.forEach(::drawNode)
                ImGui.treePop()
            }
            ImGui.popID()
        }
        drawNode(scene.rootNode)
        ImGui.end()

        ImGui.begin("Debug")
        ImGui.text("FPS: " + timer.fps)
        ImGui.text("X:" + scene.camera.position.x)
        ImGui.text("Y:" + scene.camera.position.y)
        ImGui.text("Z:" + scene.camera.position.z)
        ImGui.separator()
        performanceProfile.entries
            .sortedByDescending { it.value }
            .forEach {
                ImGui.text("${it.key}: ${it.value}ms")
            }
        ImGui.end()

        selectedNode?.apply {
            ImGui.begin("Node info")
            ImGui.text("POS:" + this.transform.translation)
            ImGui.text("ROT:" + this.transform.rotation)
            ImGui.text("SCALE:" + this.transform.scale)
            ImGui.end()
        }
    }

    override fun onPostFrame() {
        Profiler.getSections().forEach {
            performanceProfile[it.key] = it.value.duration
        }
    }

    @Subscribe
    fun onKeyPress(e: KeyPressEvent) {
        if (e.key == GLFW_KEY_ESCAPE) {
            if (guiManager.page == null)
                guiManager.page = DemoPausePage()
            else
                guiManager.page = null
        }
        if (e.key == GLFW_KEY_LEFT_ALT) {
            this.cursorVisible = !this.cursorVisible
        }
    }
}

fun main() {
    DemoApp().run()
}