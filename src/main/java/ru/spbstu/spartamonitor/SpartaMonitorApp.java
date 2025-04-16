package ru.spbstu.spartamonitor;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.util.Duration;
import ru.spbstu.spartamonitor.data.ParserEvent;

import java.io.IOException;

public class SpartaMonitorApp extends Application {

    private SpartaMonitorController controller;
    private static final int drawTimeOut = 200;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(SpartaMonitorApp.class.getResource("sparta-monitor-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("Sparta monitor");
        stage.setScene(scene);
        stage.show();

        this.controller = fxmlLoader.getController();
        this.controller.setMainStage(stage);

        Timeline timeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(0),
                        event -> this.controller.drawIteration()
                ),
                new KeyFrame(Duration.millis(drawTimeOut))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @Override
    public void stop() {
        this.controller.onClose();
        Platform.exit();
    }

    public static void main(String[] args) {
        launch();
    }
}