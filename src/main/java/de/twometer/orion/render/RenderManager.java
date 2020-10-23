package de.twometer.orion.render;

import de.twometer.orion.render.filter.IModelFilter;
import de.twometer.orion.render.model.ModelBasePart;
import de.twometer.orion.render.shading.IShadingStrategy;
import de.twometer.orion.render.shading.NopShadingStrategy;

import java.util.ArrayList;
import java.util.List;

public class RenderManager {

    private final List<IModelFilter> modelFilters = new ArrayList<>();

    private IShadingStrategy shadingStrategy = new NopShadingStrategy();

    public boolean shouldRender(ModelBasePart part) {
        for (var filter : modelFilters)
            if (!filter.shouldRender(part))
                return false;
        return true;
    }

    public void update() {
        for (var filter : modelFilters)
            filter.update();
    }

    public void addModelFilter(IModelFilter filter) {
        modelFilters.add(filter);
    }

    public IShadingStrategy getShadingStrategy() {
        return shadingStrategy;
    }

    public void setShadingStrategy(IShadingStrategy shadingStrategy) {
        this.shadingStrategy = shadingStrategy;
    }
}