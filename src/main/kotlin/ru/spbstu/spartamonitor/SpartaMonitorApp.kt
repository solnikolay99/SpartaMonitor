package ru.spbstu.spartamonitor

import javafx.animation.KeyFrame
import javafx.animation.Timeline
import javafx.application.Application
import javafx.application.Application.launch
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage
import javafx.util.Duration
import ru.spbstu.spartamonitor.eventbus.EventBusFactory
import ru.spbstu.spartamonitor.events.DrawEvent
import ru.spbstu.spartamonitor.listeners.DrawDensityListener
import ru.spbstu.spartamonitor.listeners.DrawListener
import ru.spbstu.spartamonitor.listeners.ParserListener

class SpartaMonitorApp : Application() {
    private val drawTimeout = 200
    private lateinit var controller: SpartaMonitorController

    override fun start(stage: Stage) {
        val fxmlLoader = FXMLLoader(SpartaMonitorApp::class.java.getResource("sparta-monitor-view.fxml"))
        val scene = Scene(fxmlLoader.load())
        stage.title = "Sparta monitor"
        stage.setScene(scene)
        stage.show()

        this.controller = fxmlLoader.getController()
        this.controller.setStageMain(stage)

        EventBusFactory.eventBus.register(ParserListener(this.controller))
        EventBusFactory.eventBus.register(DrawListener(this.controller))
        EventBusFactory.eventBus.register(DrawDensityListener(this.controller))

        val timeline = Timeline(
            KeyFrame(
                Duration.seconds(0.0),
                { EventBusFactory.eventBus.post(DrawEvent(null)) }
            ),
            KeyFrame(Duration.millis(drawTimeout.toDouble()))
        )
        timeline.cycleCount = Timeline.INDEFINITE
        timeline.play()
    }

    override fun stop() {
        this.controller.onClose()
        Platform.exit()
    }
}

fun main() {
    launch(SpartaMonitorApp::class.java)
}