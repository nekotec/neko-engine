import de.twometer.neko.core.AppConfig
import de.twometer.neko.core.NekoApp
import de.twometer.neko.res.AssetManager
import de.twometer.neko.res.ModelLoader
import de.twometer.neko.scene.PointLight
import de.twometer.neko.util.MathF.toRadians

class DemoApp : NekoApp(AppConfig(windowTitle = "Neko Engine Demo", debugMode = false)) {

    override fun onPreInit() {
        AssetManager.registerPath("./neko-engine/assets")
        AssetManager.registerPath("./neko-demo/assets")
    }

    override fun onPostInit() {
        val model1 = ModelLoader.loadFromFile("rin.fbx")
        model1.transform.rotation.rotateX(toRadians(-90f)).rotateZ(-1.3f)
        model1.transform.translation.set(0f, 1f, 0f)
        scene.rootNode.attachChild(model1)

        val model2 = ModelLoader.loadFromFile("animegirl.fbx")
        model2.transform.rotation.rotateX(toRadians(-90f))
        model2.transform.translation.set(2f, 0f, 0f)
        scene.rootNode.attachChild(model2)

        val model3 = ModelLoader.loadFromFile("test.fbx")
        scene.rootNode.attachChild(model3)

        scene.rootNode.attachChild(PointLight())
    }

}

fun main() {
    DemoApp().run()
}