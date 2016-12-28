package com.jayfella.sky.atmosphere;


import com.jayfella.sky.astronomy.PositionProvider;
import com.jayfella.sky.astronomy.SimplePositionProvider;
import com.jayfella.sky.atmosphere.clouds.Clouds;
import com.jayfella.sky.atmosphere.sky.Moon;
import com.jayfella.sky.atmosphere.sky.Sky;
import com.jayfella.sky.atmosphere.sky.Stars;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;


/**
 * Created by James on 12/3/2016.
 */
public class AtmosphereState extends BaseAppState {

    private Node rootNode;

    private Node atmosRoot;

    private PositionProvider positionProvider = new SimplePositionProvider();

    // private ColorRGBA nightCol;
    private ColorRGBA sunColor = ColorRGBA.White;
    private ColorRGBA ambientColor = ColorRGBA.Gray;
    private ColorGradients gradients;

    private DirectionalLight sun;
    private AmbientLight amb;

    private Sky sky;
    private Clouds clouds;

    private Moon moon;
    private Stars stars;

    private boolean sunUp = false;
    private boolean HDR = true;

    public AtmosphereState() {

    }

    @Override
    protected void initialize(Application app) {

        rootNode = ((SimpleApplication)app).getRootNode();
        atmosRoot = new Node("atmosphere");

        ColorRGBA nightCol = new ColorRGBA(0.318f, 0.345f, 0.525f, 1f).multLocal(0.1f);
        nightCol.a = 1f;

        getApplication().getViewPort().setBackgroundColor(nightCol);

        gradients = new ColorGradients();
        sky = new Sky(app.getAssetManager(), 500f, 0); // height is ignored.
        sky.setHDR(false);
        atmosRoot.attachChild(sky.getSkyDome());

        sun = new DirectionalLight();
        sun.setName("sun");

        moon = new Moon(app.getAssetManager());
        atmosRoot.attachChild(moon.getMoonGeom());

        clouds = new Clouds(app.getAssetManager(), sky);

        stars = new Stars(app.getAssetManager(), 360, 360);
        stars.load(true);
        atmosRoot.attachChild(stars.getStarGeometry());

        amb = new AmbientLight();
        amb.setColor(ambientColor);
        atmosRoot.addLight(amb);

        // sky.getSkyDome().setLocalTranslation(0, 0, 0);
        // stars.getStarGeometry().setLocalTranslation(0, 0, 0);
        atmosRoot.getLocalTranslation().addLocal(0f, -64f, 0f);
    }



    @Override
    protected void onEnable() {
        rootNode.attachChild(atmosRoot);
        rootNode.addLight(amb);
    }

    @Override
    protected void onDisable() {
        atmosRoot.removeFromParent();

        rootNode.removeLight(amb);
        rootNode.removeLight(sun);
        rootNode.removeLight(moon.getMoonLight());
    }

    @Override
    protected void cleanup(Application app) {
    }

    public boolean getUseHDR() {
        return HDR;
    }
    public void setUseHDR(boolean HDR) {
        this.HDR = HDR;
        sky.getMaterial().setBoolean("HDR", HDR);
    }

    public ColorRGBA getAmbientColor() {
        return ambientColor;
    }
    public ColorRGBA getSunColor() {
        return sun.getColor();
    }
    public ColorGradients getColorGradients() {
        return gradients;
    }

    public PositionProvider getPositionProvider() {
        return positionProvider;
    }

    public Sky getSky() {
        return sky;
    }
    public Clouds getClouds() {
        return clouds;
    }
    public DirectionalLight getSun() {
        return sun;
    }
    public Moon getMoon() {
        return moon;
    }

    public float getTimeScale(){
        return this.getPositionProvider().getCalendar().gettMult();
    }
    public void setTimeScale(float scale){
        this.positionProvider.getCalendar().setTMult(scale);
    }

    @Override
    public void update(float tpf) {

        // we don't need to update the positions, because the world moves, not the camera.
        // Vector3f camLocation = app.getCamera().getLocation();
        // sky.getSkyDome().setLocalTranslation(0, 0, 0);
        // stars.getStarGeometry().setLocalTranslation(0, 0, 0);

        stars.update(tpf);
        positionProvider.update(tpf);

        // ------ Sun ------
        Vector3f sunDirection = positionProvider.getSunDirection();

        // Get the suns height in the range 0 to 1.
        // float position = sunDirection.y / positionProvider.getMaxHeight() * 0.5f + 0.5f;
        float position = sunDirection.y / positionProvider.getMaxHeight() * 0.5f + 0.3f;
        sunColor = gradients.getSunColor(position);
        // sunColor = gradients.getSunColor(FastMath.clamp(position + 0.1f,0,1));

        //         Switch lights
        if (sunDirection.y > -0.2f) {
            if (sunUp == false) {
                rootNode.addLight(sun);
                rootNode.removeLight(moon.getMoonLight());
                sunUp = true;
            }
        } else if (sunDirection.y <= -0.2f) {
            if (sunUp == true) {
                rootNode.removeLight(sun);
                rootNode.addLight(moon.getMoonLight());
                sunUp = false;
            }
        }

        if (sunUp)
        {
            sun.setColor(sunColor.mult(0.7f));
            sun.setDirection(sunDirection.negate());
        }
        else
        {
            moon.getMoonLight().setColor(sunColor.mult(2f));
            moon.getMoonLight().setDirection(sunDirection);
        }

        moon.getMoonGeom().setLocalTranslation(sunDirection.mult(-950).addLocal(getApplication().getCamera().getLocation()));
        moon.getMoonGeom().lookAt(getApplication().getCamera().getLocation(), Vector3f.UNIT_Y);
        boolean nextIsDusk = (positionProvider.getCalendar().getHour() > 12);

        // Stuff to avoid using 2 lightsources and detach/attach stars.
        if (sunDirection.y < -0.1 && sunDirection.y >= -0.2f) {
            if (nextIsDusk) {
                sun.getColor().multLocal(1 + (sunDirection.y + 0.1f) * 10);
            } else {
                sun.getColor().multLocal((0.2f + sunDirection.y) * 10);
            }
        } else if (sunDirection.y < -0.2 && sunDirection.y >= -0.3f) {
            if (nextIsDusk) {
                moon.getMoonLight().getColor().multLocal((-sunDirection.y - 0.2f) * 10);
            } else {
                moon.getMoonLight().getColor().multLocal(1 - (sunDirection.y + 0.3f) * 10);
            }
        }

        // ------ Ambient lighting ------
        ambientColor = gradients.getSkyAmbientColor(FastMath.clamp(position + 0.1f, 0, 1));
        // ambientColor = gradients.getSkyAmbientColor(FastMath.clamp(position , 0, 1));
        amb.setColor(ambientColor.mult(2f));

        // ------ Update Materials ------
        Material skyMat = sky.getMaterial();
        skyMat.setVector3("LightDir", sunDirection);
        skyMat.setColor("SunColor", sunColor);
        skyMat.setColor("AmbientColor", ambientColor);

        // ------ Fog ------

        // if (fogManager != null) {
        // fogManager.update(this, sunDirection, position);
        // }

    }

}
