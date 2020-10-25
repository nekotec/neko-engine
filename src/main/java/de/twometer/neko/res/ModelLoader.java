package de.twometer.neko.res;

import de.twometer.neko.core.NekoApp;
import de.twometer.neko.render.Color;
import de.twometer.neko.render.model.*;
import de.twometer.neko.util.Log;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;

public class ModelLoader {

    public static ModelBase loadModel(String modelFile) {
        return new CompositeModel(modelFile, loadModels(modelFile));
    }

    public static List<ModelBase> loadModels(String modelFile) {
        Log.d("Loading model " + modelFile);
        String path = AssetPaths.MODEL_PATH + modelFile;
        AIScene aiScene = aiImportFile(path, 0);
        if (aiScene == null) {
            throw new IllegalStateException(aiGetErrorString());
        }

        int numMaterials = aiScene.mNumMaterials();
        PointerBuffer aiMaterials = aiScene.mMaterials();
        List<Material> materials = new ArrayList<>();
        for (int i = 0; i < numMaterials; i++) {
            AIMaterial aiMaterial = AIMaterial.create(aiMaterials.get(i));
            AIString matname = AIString.calloc();
            aiGetMaterialString(aiMaterial, AI_MATKEY_NAME, 0, 0, matname);

            AIString texpath = AIString.calloc();
            aiGetMaterialTexture(aiMaterial, aiTextureType_DIFFUSE, 0, texpath, (IntBuffer) null, null, null, null, null, null);

            AIColor4D aiDiffuseColor = AIColor4D.create();
            aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, aiDiffuseColor);
            Color diffuseColor = new Color(aiDiffuseColor.r(), aiDiffuseColor.g(), aiDiffuseColor.b(), aiDiffuseColor.a());

            AIColor4D aiEmissiveColor = AIColor4D.create();
            aiGetMaterialColor(aiMaterial, AI_MATKEY_COLOR_EMISSIVE, aiTextureType_NONE, 0, aiEmissiveColor);
            Color emissiveColor = new Color(aiEmissiveColor.r(), aiEmissiveColor.g(), aiEmissiveColor.b(), aiEmissiveColor.a());

            var buf = PointerBuffer.allocateDirect(1);
            aiGetMaterialProperty(aiMaterial, AI_MATKEY_OPACITY, buf);
            var prop = AIMaterialProperty.create(buf.get());
            var op = prop.mData().asFloatBuffer().get();
            if (op < diffuseColor.getA())
                diffuseColor = new Color(diffuseColor.getR(), diffuseColor.getG(), diffuseColor.getB(), op);


            var texturePath = texpath.dataString();
            var texture = !texturePath.isEmpty() ? NekoApp.get().getTextureProvider().get(texturePath) : null;

            materials.add(new Material(matname.dataString(), texture, diffuseColor, emissiveColor));

            /*matname.free();
            texpath.free();
            aiDiffuseColor.free();
            aiEmissiveColor.free();*/
        }

        PointerBuffer aiMeshes = aiScene.mMeshes();
        int numMeshes = aiScene.mNumMeshes();

        String currentName = "";
        List<ModelBase> result = new ArrayList<>();
        List<ModelBase> currentSubModels = new ArrayList<>();

        for (int i = 0; i < numMeshes; i++) {
            AIMesh aiMesh = AIMesh.create(aiMeshes.get(i));

            String name = aiMesh.mName().dataString();

            if (name.startsWith("OBJ_")) { // actual original object
                mergeModels(currentName, currentSubModels, result);

                currentName = name.substring("OBJ_".length());
            }

            currentSubModels.add(convert(name, aiMesh, materials));
        }

        mergeModels(currentName, currentSubModels, result);
        return result;
    }

    private static void mergeModels(String name, List<ModelBase> input, List<ModelBase> output) {
        if (input.size() != 0) {
            ModelBase model = input.size() == 1 ? input.get(0) : new CompositeModel(name, new ArrayList<>(input));
            Log.d("Merged " + input.size() + " parts into model " + name);
            output.add(model);
            input.clear();
        }
    }

    private static ModelBase convert(String name, AIMesh aiMesh, List<Material> mats) {
        Mesh mesh = Mesh.create(aiMesh.mNumVertices(), 3)
                .withTexCoords()
                .withNormals();
        AIVector3D.Buffer aiVertices = aiMesh.mVertices();
        while (aiVertices.remaining() > 0) {
            AIVector3D aiVertex = aiVertices.get();
            mesh.putVertex(aiVertex.x(), aiVertex.y(), aiVertex.z());
        }

        AIVector3D.Buffer aiNormals = aiMesh.mNormals();
        while (aiNormals.remaining() > 0) {
            AIVector3D aiNormal = aiNormals.get();
            mesh.putNormal(aiNormal.x(), aiNormal.y(), aiNormal.z());
        }

        AIVector3D.Buffer aiTexCoords = aiMesh.mTextureCoords(0);
        while (aiTexCoords.remaining() > 0) {
            AIVector3D aiTexCoord = aiTexCoords.get();
            mesh.putTexCoord(aiTexCoord.x(), 1 - aiTexCoord.y());
        }

        ModelPart model = mesh.bake(name, GL_TRIANGLES);
        model.setMaterial(mats.get(aiMesh.mMaterialIndex()));
        return model;
    }

}