package de.twometer.neko.res

import de.twometer.neko.scene.*
import mu.KotlinLogging
import org.lwjgl.PointerBuffer
import org.lwjgl.assimp.*
import org.lwjgl.assimp.Assimp.*
import java.io.File
import java.nio.IntBuffer

private val logger = KotlinLogging.logger {}

object ModelLoader {

    fun load(path: String): Node {
        logger.info { "Loading model $path" }

        val modelFileName = File(path).name

        val file = AssetManager.resolve(path, AssetType.Models)
        val flags = aiProcess_Triangulate or
                aiProcess_JoinIdenticalVertices or
                aiProcess_RemoveRedundantMaterials or
                aiProcess_ImproveCacheLocality or
                aiProcess_FixInfacingNormals or
                aiProcess_FindInvalidData or
                aiProcess_LimitBoneWeights or
                aiProcess_SortByPType or
                aiProcess_GenSmoothNormals or
                aiProcess_GenUVCoords

        val aiScene = aiImportFile(file.absolutePath, flags) ?: failure()

        val animations = ArrayList<Animation>()
        val materials = ArrayList<Material>()
        val node = Node(name = modelFileName, animations = animations)

        aiScene.mAnimations()?.also {
            val aiNumAnimations = aiScene.mNumAnimations()
            for (i in 0 until aiNumAnimations) {
                val aiAnimation = AIAnimation.create(it[i])
                val animation = createAnimation(aiAnimation)
                animations.add(animation)
            }
        }

        aiScene.mMaterials()?.also {
            val aiNumMaterials = aiScene.mNumMaterials()
            for (i in 0 until aiNumMaterials) {
                val aiMaterial = AIMaterial.create(it[i])
                val material = createMaterial(modelFileName, aiMaterial)
                materials.add(material)
            }
        }

        aiScene.mMeshes()?.also {
            val aiNumMeshes = aiScene.mNumMeshes()
            for (i in 0 until aiNumMeshes) {
                val aiMesh = AIMesh.create(it[i])
                val material = materials[aiMesh.mMaterialIndex()]
                val geometry = createMesh(aiMesh).toGeometry(material)

                node.attachChild(geometry)
            }
        }

        return node
    }

    private fun createAnimation(aiAnimation: AIAnimation): Animation {
        val animation = Animation(aiAnimation.mTicksPerSecond(), aiAnimation.mDuration())

        logger.debug {
            "Loading animation ${
                aiAnimation.mName().dataString()
            } (${aiAnimation.mNumChannels()} channels)"
        }

        aiAnimation.mChannels()?.also { aiChannels ->
            val aiNumChannels = aiAnimation.mNumChannels()
            for (i in 0 until aiNumChannels) {
                val channel = AINodeAnim.create(aiChannels[i])
                println(channel.mNodeName().dataString())
            }
        }

        return animation
    }

    private fun createMesh(aiMesh: AIMesh): Mesh {
        val name = aiMesh.mName().dataString()
        logger.debug {
            "Loading mesh $name (${aiMesh.mNumVertices()} vertices, ${aiMesh.mNumFaces()} tris, ${aiMesh.mNumBones()} bones)"
        }

        val mesh = Mesh(aiMesh.mNumVertices(), 3, ModelProperties(name))
            .addIndices(aiMesh.mNumFaces() * 3)
            .addTexCoords()
            .addNormals()

        aiMesh.mVertices().forEach {
            mesh.putVertex(it.x(), it.y(), it.z())
        }

        aiMesh.mTextureCoords(0)?.forEach {
            mesh.putTexCoord(it.x(), 1 - it.y())
        }

        aiMesh.mNormals()?.forEach {
            mesh.putNormal(it.x(), it.y(), it.z())
        }

        val aiBones = aiMesh.mBones()
        aiBones?.apply {
            // TODO Parse bones
            while (aiBones.hasRemaining())
                AIBone.create(aiBones.get())
        }

        val aiFaces = aiMesh.mFaces()
        while (aiFaces.hasRemaining())
            aiFaces.get().also {
                mesh.putIndices(it.mIndices()[0], it.mIndices()[1], it.mIndices()[2])
            }
        return mesh
    }

    private fun createMaterial(modelFile: String, aiMaterial: AIMaterial): Material {
        val aiMatName = AIString.create()
        aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, aiMatName)

        val material = Material(aiMatName.dataString())
        logger.debug { "Loading material ${material.name}" }

        material[MatKey.ColorAmbient] = aiMaterial.getColor(AI_MATKEY_COLOR_AMBIENT)
        material[MatKey.ColorDiffuse] = aiMaterial.getColor(AI_MATKEY_COLOR_DIFFUSE)
        material[MatKey.ColorEmissive] = aiMaterial.getColor(AI_MATKEY_COLOR_EMISSIVE)
        material[MatKey.ColorSpecular] = aiMaterial.getColor(AI_MATKEY_COLOR_SPECULAR)

        material[MatKey.TextureAmbient] = aiMaterial.getTexture(modelFile, aiTextureType_AMBIENT)
        material[MatKey.TextureDiffuse] = aiMaterial.getTexture(modelFile, aiTextureType_DIFFUSE)
        material[MatKey.TextureEmissive] = aiMaterial.getTexture(modelFile, aiTextureType_EMISSIVE)
        material[MatKey.TextureSpecular] = aiMaterial.getTexture(modelFile, aiTextureType_SPECULAR)
        material[MatKey.TextureDisplacement] = aiMaterial.getTexture(modelFile, aiTextureType_DISPLACEMENT)
        material[MatKey.TextureNormals] = aiMaterial.getTexture(modelFile, aiTextureType_NORMALS)

        material[MatKey.Reflectivity] = aiMaterial.getFloat(AI_MATKEY_REFLECTIVITY)
        material[MatKey.Opacity] = aiMaterial.getFloat(AI_MATKEY_OPACITY)
        material[MatKey.Shininess] = aiMaterial.getFloat(AI_MATKEY_SHININESS)

        return material
    }

    private fun failure(): Nothing {
        error(aiGetErrorString() ?: "Unknown Assimp error")
    }

    private fun processTexturePath(modelFile: String, texturePath: String): String? {
        if (texturePath.isBlank())
            return null

        val path = "$modelFile/${texturePath.substringAfterLast("/").substringAfterLast("\\")}"

        return if (AssetManager.exists(path, AssetType.Textures)) {
            path
        } else {
            logger.warn { "Cannot find texture for model $modelFile at $path" }
            null
        }
    }

    private fun AIMaterial.getColor(matKey: String): Color {
        val color = AIColor4D.create()
        aiGetMaterialColor(this, matKey, aiTextureType_NONE, 0, color)
        return Color(color.r(), color.g(), color.b(), color.a())
    }

    private fun AIMaterial.getTexture(modelFile: String, textureType: Int): String? {
        val path = AIString.create()
        aiGetMaterialTexture(this, textureType, 0, path, null as IntBuffer?, null, null, null, null, null)
        return processTexturePath(modelFile, path.dataString())
    }

    private fun AIMaterial.getFloat(matKey: String): Float {
        val pointerBuffer = PointerBuffer.allocateDirect(1)
        aiGetMaterialProperty(this, matKey, pointerBuffer)
        val property = AIMaterialProperty.create(pointerBuffer.get())

        // Ignore null pointer addresses
        if (property.address() == 0L) return 0F

        // Read the float
        val floatBuffer = property.mData().asFloatBuffer()
        return floatBuffer.get()
    }

}