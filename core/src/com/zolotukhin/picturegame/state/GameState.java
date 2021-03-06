package com.zolotukhin.picturegame.state;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.zolotukhin.picturegame.GameManager;
import com.zolotukhin.picturegame.builder.ButtonBuilder;
import com.zolotukhin.picturegame.factory.FallItemFactory;
import com.zolotukhin.picturegame.factory.SimpleFallItemFactory;
import com.zolotukhin.picturegame.gameobject.Button;
import com.zolotukhin.picturegame.gameobject.FallingItem;
import com.zolotukhin.picturegame.gameobject.Floor;
import com.zolotukhin.picturegame.gameobject.GameObject;
import com.zolotukhin.picturegame.gameobject.Hud;
import com.zolotukhin.picturegame.gameobject.Player;

import java.util.Iterator;

/**
 * Created by Artem Zolotukhin on 7/9/17.
 */

public class GameState extends State implements Button.ButtonEventListener {


    private static final float MIN_SPACE_INTERVAL_SPAWN_ITEM = 0.2f;

    private static final float START_SPACE_INTERVAL_SPAWN_ITEM = 1.0f;

    private static final float STEP_FACTOR_SPACE_INTERVAL_SPAWN_ITEM = 0.99f;

    public static final float BOTTOM_PANEL_HEIGHT = 0.15f;
    public static final float PAUSE_BUTTON_SIZE = 0.12f;
    public static final float ARROW_BUTTON_WIDTH = 0.44f;
    public static final float ARROW_BUTTON_HEIGHT = 0.12f;

    private Player player;

    private Array<FallingItem> fallingItems;
    private Array<GameObject> simpleObjects;

    private float spaceInterval;
    private FallingItem lastItem;
    private FallItemFactory fallItemFactory;

    private Hud hud;
    private Button btnPause, btnArrowLeft, btnArrowRight;
    private Floor floor;

    private boolean isLeftPressed, isRightPressed;


    public GameState(GameManager gsm) {
        super(gsm);

        simpleObjects = new Array<>();
        float unit = gsm.getScreenWidth();

        camera.setToOrtho(false, gsm.getScreenWidth(), gsm.getScreenHeight());

        player = new Player(gsm.getScreenWidth() / 2, BOTTOM_PANEL_HEIGHT * unit + 4, unit);

        fallItemFactory = new SimpleFallItemFactory(gsm.getScreenWidth(), gsm.getScreenHeight(), player);

        fallingItems = new Array<>();

        spaceInterval = START_SPACE_INTERVAL_SPAWN_ITEM;

        int fontSize = Math.round(unit * 0.05f);
        int hudMargin = 24;


        hud = new Hud(hudMargin, gsm.getScreenHeight() - fontSize, fontSize);
        simpleObjects.add(hud);

        isLeftPressed = false;
        isRightPressed = false;

        btnPause = new ButtonBuilder()
                .textureSimple(new Texture("btn_pause_simple.png"), true)
                .texturePressed(new Texture("btn_pause_pressed.png"), true)
                .height(PAUSE_BUTTON_SIZE * unit)
                .width(PAUSE_BUTTON_SIZE * unit)
                .state(this)
                .addEventListener(this)
                .build();
        btnPause.setX((gsm.getScreenWidth() - btnPause.getWidth()) / 2);
        btnPause.setY((BOTTOM_PANEL_HEIGHT - PAUSE_BUTTON_SIZE) / 2 * unit);
        simpleObjects.add(btnPause);

        btnArrowLeft = new ButtonBuilder()
                .textureSimple(new Texture("btn_left_simple.png"), true)
                .texturePressed(new Texture("btn_left_pressed.png"), true)
                .width(ARROW_BUTTON_WIDTH * unit)
                .height(ARROW_BUTTON_HEIGHT * unit)
                .x(0).y((BOTTOM_PANEL_HEIGHT - ARROW_BUTTON_HEIGHT) / 2 * unit)
                .state(this)
                .addEventListener(this)
                .build();
        simpleObjects.add(btnArrowLeft);

        btnArrowRight = new ButtonBuilder()
                .textureSimple(new Texture("btn_right_simple.png"), true)
                .texturePressed(new Texture("btn_right_pressed.png"), true)
                .width(ARROW_BUTTON_WIDTH * unit)
                .height(ARROW_BUTTON_HEIGHT * unit)
                .x(gsm.getScreenWidth() - ARROW_BUTTON_WIDTH * unit)
                .y((BOTTOM_PANEL_HEIGHT - ARROW_BUTTON_HEIGHT) / 2 * unit)
                .state(this)
                .addEventListener(this)
                .build();
        simpleObjects.add(btnArrowRight);


        floor = new Floor(0, 0, gsm.getScreenWidth(), BOTTOM_PANEL_HEIGHT * unit);
        simpleObjects.add(floor);
    }


    @Override
    public void update(float delta) {

        checkPlayerMovementControl(delta);

        checkAndSpawnFallingItem();
        updateFallingItems(delta);
        updateSimpleObjects(delta);

        player.update(delta);

        hud.setPoints(player.getPoints());
        hud.setLives(player.getLives());

        if (player.getLives() <= 0) {
            gsm.setState(new GameOverState(gsm));
        }
    }

    private void checkPlayerMovementControl(float delta) {
        if (isLeftPressed) {
            player.move(Player.Direction.LEFT, delta);
        } else {
            if (isRightPressed) {
                player.move(Player.Direction.RIGHT, delta);
            }
        }
        isLeftPressed = false;
        isRightPressed = false;
    }

    private void updateSimpleObjects(float delta) {
        Iterator<GameObject> iterator = simpleObjects.iterator();
        GameObject object;
        while (iterator.hasNext()) {
            object = iterator.next();
            object.update(delta);
            if (object.isDead()) {
                iterator.remove();
            }
        }
    }

    private void updateFallingItems(float delta) {
        Iterator<FallingItem> iterator = fallingItems.iterator();
        FallingItem item;
        while (iterator.hasNext()) {
            item = iterator.next();
            item.update(delta);
            if (item.isDead()) {
                item.dispose();
                iterator.remove();
            }
        }
        btnPause.update(delta);
    }

    private void checkAndSpawnFallingItem() {

        if (lastItem == null) {
            lastItem = fallItemFactory.getItem();
            fallingItems.add(lastItem);
        } else {

            if (lastItem.getY() < gsm.getScreenHeight() - gsm.getScreenWidth() * spaceInterval - lastItem.getHeight()) {

                lastItem = fallItemFactory.getItem();
                fallingItems.add(lastItem);

                if (spaceInterval > MIN_SPACE_INTERVAL_SPAWN_ITEM) {
                    spaceInterval *= STEP_FACTOR_SPACE_INTERVAL_SPAWN_ITEM;
                } else {
                    spaceInterval = MIN_SPACE_INTERVAL_SPAWN_ITEM;
                }
            }
        }
        lastItem.setFloor(floor);
    }

    @Override
    public void render(SpriteBatch batch) {

        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        player.renderWithoutBeginEnd(batch);

        for (FallingItem i : fallingItems) {
            i.renderWithoutBeginEnd(batch);
        }

        renderSimpleObjects(batch);

        batch.end();
    }

    private void renderSimpleObjects(SpriteBatch batch) {
        for (GameObject i : simpleObjects) {
            i.renderWithoutBeginEnd(batch);
        }
    }

    @Override
    public void pause() {

        gsm.pushState(new PauseState(gsm));
    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        for (FallingItem i : fallingItems) {
            i.dispose();
        }

        player.dispose();
        hud.dispose();
    }

    @Override
    public void onEvent(Button button, Button.Event event) {

        switch (event) {
            case RELEASED:
                if (button == btnPause) {
                    gsm.pushState(new PauseState(gsm));
                }
                break;
            case HOLDING:
               if (button == btnArrowLeft) {
                   isLeftPressed = true;
                   break;
               }
               if (button == btnArrowRight) {
                   isRightPressed = true;
                   break;
               }
               break;
        }


    }
}
