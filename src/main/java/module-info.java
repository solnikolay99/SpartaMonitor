module ru.spbstu.spartamonitor {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;
    requires java.desktop;

    opens ru.spbstu.spartamonitor to javafx.fxml;
    exports ru.spbstu.spartamonitor;
    exports ru.spbstu.spartamonitor.colorize;
    exports ru.spbstu.spartamonitor.data;
    exports ru.spbstu.spartamonitor.data.models;
    opens ru.spbstu.spartamonitor.data to javafx.fxml;
    exports ru.spbstu.spartamonitor.logger;
    opens ru.spbstu.spartamonitor.logger to javafx.fxml;
}