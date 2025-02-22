package dev.lucasnlm.antimine.gdx.stages

import android.util.SizeF
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import dev.lucasnlm.antimine.core.models.Area
import dev.lucasnlm.antimine.gdx.BuildConfig
import dev.lucasnlm.antimine.gdx.GdxLocal
import dev.lucasnlm.antimine.gdx.PixelPerfectViewport
import dev.lucasnlm.antimine.gdx.actors.AreaActor
import dev.lucasnlm.antimine.gdx.controller.CameraController
import dev.lucasnlm.antimine.gdx.events.GdxEvent
import dev.lucasnlm.antimine.gdx.models.ActionSettings
import dev.lucasnlm.antimine.gdx.models.RenderSettings
import dev.lucasnlm.antimine.preferences.models.Minefield

class MinefieldStage(
    val screenWidth: Float,
    val screenHeight: Float,
    private var actionSettings: ActionSettings,
    private val renderSettings: RenderSettings,
    private val onSingleTap: (Int) -> Unit,
    private val onDoubleTap: (Int) -> Unit,
    private val onLongTouch: (Int) -> Unit,
    private val onEngineReady: () -> Unit,
) : Stage(PixelPerfectViewport(screenWidth, screenHeight)) {
    private var minefield: Minefield? = null
    private var minefieldSize: SizeF? = null
    private var currentZoom: Float = 1.0f

    private var lastCameraPosition: Vector3? = null
    private var lastZoom: Float? = null

    private val cameraController: CameraController

    private var forceRefreshVisibleAreas = true
    private var boundHashCode: Int? = null
    private var boundAreas = listOf<Area>()

    private var inputInit: Long = 0L
    private val inputEvents: MutableList<GdxEvent> = mutableListOf()

    init {
        actionsRequestRendering = true

        addListener(object : InputListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                return if (event?.target is Group) {
                    event.cancel()
                    true
                } else {
                    false
                }
            }
        })

        cameraController = CameraController(
            camera = camera,
            renderSettings = renderSettings,
        )
    }

    fun setZoom(value: Float) {
        (camera as OrthographicCamera).apply {
            zoom = value.coerceIn(0.8f, 3.0f)
            currentZoom = zoom
            update(true)

            GdxLocal.zoomLevelAlpha = when {
                zoom < 3.5f -> {
                    1.0f
                }
                zoom > 4.0f -> {
                    0.0f
                }
                else -> {
                    (3.5f - zoom)
                }
            }
        }

        inputEvents.clear()
    }

    fun scaleZoom(zoomMultiplier: Float) {
        (camera as OrthographicCamera).apply {
            val newZoom = if (zoomMultiplier > 1.0) {
                zoom + 2.0f * Gdx.graphics.deltaTime
            } else {
                zoom - 2.0f * Gdx.graphics.deltaTime
            }
            zoom = newZoom.coerceIn(0.8f, 3.0f)
            currentZoom = zoom
            update(true)

            GdxLocal.zoomLevelAlpha = when {
                zoom < 3.5f -> {
                    1.0f
                }
                zoom > 4.0f -> {
                    0.0f
                }
                else -> {
                    (3.5f - zoom)
                }
            }
        }

        inputEvents.clear()
    }

    fun bindField(field: List<Area>) {
        boundAreas = field
        forceRefreshVisibleAreas = true
    }

    private fun refreshAreas(visibleHash: Int?) {
        if (visibleHash != boundHashCode && visibleHash != null || forceRefreshVisibleAreas) {
            if (visibleHash != null) {
                boundHashCode = visibleHash
            }

            boundAreas.let { field ->
                if (actors.size != field.size) {
                    clear()
                    field.map {
                        AreaActor(
                            theme = renderSettings.theme,
                            size = renderSettings.areaSize,
                            area = it,
                            field = field,
                            onInputEvent = ::handleGameEvent,
                            enableLigatures = renderSettings.joinAreas,
                        )
                    }.forEach(::addActor)

                    camera.update(true)
                    refreshVisibleActorsIfNeeded()
                } else {
                    val reset = field.count { it.hasMine } == 0

                    actors.forEach {
                        if (it.isVisible) {
                            val areaActor = (it as AreaActor)
                            val area = field[areaActor.boundAreaId()]
                            areaActor.bindArea(reset, renderSettings.joinAreas, area, field)
                        }
                    }
                }
            }

            onEngineReady()

            forceRefreshVisibleAreas = false
            Gdx.graphics.requestRendering()
        }
    }

    fun bindSize(newMinefield: Minefield?) {
        minefield = newMinefield
        minefieldSize = newMinefield?.let {
            SizeF(
                it.width * renderSettings.areaSize,
                it.height * renderSettings.areaSize,
            )
        }
        onChangeGame()
    }

    private fun centerCamera() {
        this.minefieldSize?.let {
            val virtualWidth = Gdx.graphics.width
            val virtualHeight = Gdx.graphics.height
            val padding = renderSettings.internalPadding

            val start = 0.5f * virtualWidth - padding.start
            val end = it.width - 0.5f * virtualWidth + padding.end
            val top = it.height - 0.5f * (virtualHeight - padding.top)
            val bottom = 0.5f * virtualHeight + padding.bottom - renderSettings.navigationBarHeight

            camera.run {
                position.set((start + end) * 0.5f, (top + bottom) * 0.5f, 0f)
                update(true)
            }

            Gdx.graphics.requestRendering()
        }
    }

    fun onChangeGame() {
        centerCamera()
    }

    private fun handleGameEvent(gdxEvent: GdxEvent) {
        if (inputEvents.firstOrNull { it.id != gdxEvent.id } != null) {
            inputEvents.clear()
        }

        if (inputEvents.firstOrNull { it is GdxEvent.TouchUpEvent } == null) {
            inputInit = System.currentTimeMillis()
        }

        if (gdxEvent is GdxEvent.TouchUpEvent && inputEvents.firstOrNull { it is GdxEvent.TouchDownEvent } == null) {
            // Ignore unpaired up event
            return
        }

        inputEvents.add(gdxEvent)
    }

    private fun checkGameTouchInput(now: Long) {
        if (inputEvents.isNotEmpty()) {
            val dt = now - inputInit

            val touchUpEvents = inputEvents.filterIsInstance<GdxEvent.TouchUpEvent>()
            val touchDownEvents = inputEvents.filterIsInstance<GdxEvent.TouchDownEvent>()

            if (touchUpEvents.isNotEmpty()) {
                if (touchUpEvents.size == touchDownEvents.size) {
                    if (actionSettings.handleDoubleTaps) {
                        if (dt > actionSettings.doubleTapTimeout) {
                            touchUpEvents.groupBy { it.id }
                                .entries
                                .first()
                                .let {
                                    when (it.value.count()) {
                                        1 -> onSingleTap(it.key)
                                        2 -> onDoubleTap(it.key)
                                        else -> {
                                        }
                                    }
                                }.also {
                                    inputEvents.clear()
                                }
                        }
                    } else {
                        touchUpEvents.map { it.id }
                            .first()
                            .run(onSingleTap)
                            .also {
                                inputEvents.clear()
                            }
                    }
                }

                Gdx.graphics.requestRendering()
            } else if (touchDownEvents.isNotEmpty()) {
                if (dt > actionSettings.longTapTimeout) {
                    touchDownEvents.map { it.id }
                        .first()
                        .run(onLongTouch)
                        .also {
                            inputEvents.clear()
                        }
                }

                Gdx.graphics.requestRendering()
            }
        }
    }

    override fun act() {
        super.act()

        checkGameTouchInput(System.currentTimeMillis())

        // Handle camera movement
        minefieldSize?.let { cameraController.act(it) }

        val visibleHash = refreshVisibleActorsIfNeeded()

        refreshAreas(visibleHash)

        if (BuildConfig.DEBUG) {
            Gdx.app.log("GDX", "GDX FPS = ${Gdx.graphics.framesPerSecond}")
        }
    }

    private fun refreshVisibleActorsIfNeeded(): Int? {
        val camera = camera as OrthographicCamera
        var visibleHash: Int? = null
        val cameraChanged: Boolean = !camera.position.epsilonEquals(lastCameraPosition) || lastZoom != camera.zoom
        if (cameraChanged || forceRefreshVisibleAreas) {
            lastCameraPosition = camera.position.cpy()
            lastZoom = camera.zoom

            visibleHash = actors.filter {
                it.isVisible = camera.frustum.boundsInFrustum(it.x, it.y, 0f, it.width, it.height, 0.0f)
                it.isVisible
            }.hashCode()
        }

        if (BuildConfig.DEBUG) {
            val visibleCount = actors.count { it.isVisible }
            Gdx.app.log("GDX", "GDX count = $visibleCount")
        }

        return visibleHash
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        cameraController.freeTouch(screenX.toFloat(), screenY.toFloat())
        return super.touchUp(screenX, screenY, pointer, button)
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return minefieldSize?.let {
            val dx = Gdx.input.deltaX.toFloat()
            val dy = Gdx.input.deltaY.toFloat()

            if (dx * dx + dy * dy > actionSettings.touchSensibility * 8) {
                inputEvents.clear()
            }

            cameraController.startTouch(
                x = screenX.toFloat(),
                y = screenY.toFloat(),
            )

            cameraController.translate(
                dx = -dx * currentZoom,
                dy = dy * currentZoom,
                x = screenX.toFloat(),
                y = screenY.toFloat(),
            )

            true
        } != null
    }

    fun updateActionSettings(actionSettings: ActionSettings) {
        this.actionSettings = actionSettings
    }
}
