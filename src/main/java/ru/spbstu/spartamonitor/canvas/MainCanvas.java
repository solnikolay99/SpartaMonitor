package ru.spbstu.spartamonitor.canvas;

import config.Config;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import ru.spbstu.spartamonitor.colorize.ColorSchema;
import ru.spbstu.spartamonitor.colorize.ColorizeType;
import ru.spbstu.spartamonitor.data.FrameGenerator;
import ru.spbstu.spartamonitor.data.Parser;
import ru.spbstu.spartamonitor.data.models.Point;
import ru.spbstu.spartamonitor.data.models.Polygon;
import ru.spbstu.spartamonitor.eventbus.EventBusFactory;
import ru.spbstu.spartamonitor.events.DrawDensityEvent;
import ru.spbstu.spartamonitor.events.DrawEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static config.Config.shapeX;
import static config.ConfigKt.MAX_BOX_X;
import static config.ConfigKt.MAX_BOX_Y;

public class MainCanvas extends Canvas {

    private float zoom = 1f;

    private double animatedCanvasX = 0;
    private double animatedCanvasY = 0;
    private int originalShiftX = 0;
    private int originalShiftY = 0;

    private ColorizeType curColorizeType = ColorizeType.DENSITY_STATIC;

    public MainCanvas() {
        this.setOnScroll(this::onZooming);
        this.setOnMousePressed(canvasOnMousePressedEventHandler);
        this.setOnMouseReleased(canvasOnMouseReleasedEventHandler);
        this.setOnMouseDragged(canvasOnMouseDraggedEventHandler);
    }

    public void drawIteration(FrameGenerator frameGenerator,
                              FrameGenerator.Frame frame,
                              ColorizeType colorizeType,
                              boolean flgDrawByPointsOrCells,
                              String title) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setImageSmoothing(true);

        drawMask(frameGenerator);

        curColorizeType = colorizeType;
        if (flgDrawByPointsOrCells) {
            colorizePoints(frame, colorizeType);
        } else {
            colorizeCells(frame, colorizeType);
        }

        drawTitle(title);
    }

    protected void drawAxes() {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setFill(Color.GRAY);

        for (int i = 0; i <= Config.shapeX; i++) {
            gc.fillRect(i * Config.multiplayer - 1, Config.shiftBoxY + Config.mainBoxY - 6, 1, 6);
        }
        for (int i = 0; i <= shapeX; i += 5) {
            gc.fillRect(i * Config.multiplayer - 1, Config.shiftBoxY + Config.mainBoxY - 10, 1, 10);
        }
    }

    protected void drawZoom() {
        GraphicsContext gc = this.getGraphicsContext2D();

        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, 45, 22);
        gc.setFill(Color.WHITE);
        gc.fillText(String.format("%.2f X", zoom), 5, 15);
    }

    protected void drawTitle(String title) {
        GraphicsContext gc = this.getGraphicsContext2D();

        int middle = (int) gc.getCanvas().getWidth() / 2;
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(middle - 50, 0, 100, 22);
        gc.setFill(Color.WHITE);
        gc.fillText(title, middle - ((float) title.length() * 5 / 2), 15);
    }

    public void drawMask(FrameGenerator frameGenerator) {
        GraphicsContext gc = this.getGraphicsContext2D();
        gc.setImageSmoothing(true);
        gc.clearRect(0, 0, this.getWidth(), this.getHeight());
        gc.setFill(Color.AZURE);
        gc.fillRect(Config.shiftBoxX, Config.shiftBoxY, Config.mainBoxX, Config.mainBoxY);

        for (List<Polygon> surfs : frameGenerator.getSurfs().values()) {
            for (Polygon surf : surfs) {
                List<Double> xs = new ArrayList<>();
                List<Double> ys = new ArrayList<>();
                for (Point surfPoint : surf.getPoints()) {
                    xs.add((double) (Config.shiftBoxX + surfPoint.getX() * Config.multiplayer));
                    ys.add((double) (Config.shiftBoxY + surfPoint.getY() * Config.multiplayer));
                }
                gc.setFill(Color.GRAY);
                gc.fillPolygon(xs.stream().mapToDouble(Double::doubleValue).toArray(),
                        ys.stream().mapToDouble(Double::doubleValue).toArray(),
                        xs.size());
            }
        }

        drawAxes();
        drawZoom();
    }

    protected Float getDiffByDulov(FrameGenerator.Frame frame, int cellId) {
        if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                float origCellValue = frame.timeframe.getGrid().getCells().get(cellId)[0];
                float dulovsValue = FrameGenerator.dulovsPressureData.get(cellId);
                return Math.abs(origCellValue / dulovsValue * 100);
            }
        } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
            if (FrameGenerator.dulovsPressureData.containsKey(cellId)) {
                float origCellValue = frame.timeframe.getGrid().getCells().get(cellId)[7];
                float dulovsValue = FrameGenerator.dulovsPressureData.get(cellId);
                return Math.abs(origCellValue / dulovsValue * 100);
            }
        } else if (curColorizeType == ColorizeType.NRHO_DIF) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                float origCellValue = frame.timeframe.getGrid().getCells().get(cellId)[6];
                float dulovsValue = FrameGenerator.dulovsNConcentrationData.get(cellId);
                return Math.abs(origCellValue / dulovsValue * 100);
            }
        }
        return null;
    }

    protected Float getDulovData(int cellId) {
        if (curColorizeType == ColorizeType.NRHO_DULOV) {
            if (FrameGenerator.dulovsNConcentrationData.containsKey(cellId)) {
                return FrameGenerator.dulovsNConcentrationData.get(cellId);
            }
        }
        return null;
    }

    protected Color getColorForType(FrameGenerator.Frame frame, int cellId, ColorizeType colorizeType) {
        assert colorizeType != null;

        Float value = switch (colorizeType) {
            case DENSITY_STATIC -> frame.timeframe.getGrid().getCells().get(cellId)[0];
            case TEMPERATURE -> frame.timeframe.getGrid().getCells().get(cellId)[1];
            case VELOCITY -> frame.timeframe.getGrid().getCells().get(cellId)[2];
            case SOUND_VELOCITY -> frame.timeframe.getGrid().getCells().get(cellId)[3];
            case MACH -> frame.timeframe.getGrid().getCells().get(cellId)[4];
            case BIND -> (float) frame.timeframe.getGrid().getProcs().get(cellId);
            case N_COUNT -> frame.timeframe.getGrid().getCells().get(cellId)[5];
            case NRHO -> frame.timeframe.getGrid().getCells().get(cellId)[6];
            case DENSITY_STATIC_DIF, DENSITY_DYNAMIC_DIF, NRHO_DIF -> getDiffByDulov(frame, cellId);
            case NRHO_DULOV -> getDulovData(cellId);
            case DENSITY_DYNAMIC -> frame.timeframe.getGrid().getCells().get(cellId)[7];
        };

        if (value == null) {
            return null;
        } else if (value > colorizeType.getMaxValue()) {
            return ColorSchema.INSTANCE.getColorSchema().getLast();
        } else if (value < colorizeType.getMinValue()) {
            return ColorSchema.INSTANCE.getColorSchema().getFirst();
        }
        return ColorSchema.INSTANCE.getColorSchema().get((int) (value * (ColorSchema.INSTANCE.getColorSchema().size() - 1) / (colorizeType.getMaxValue() - colorizeType.getMinValue())));
    }

    protected void colorizePoints(FrameGenerator.Frame frame, ColorizeType colorizeType) {
        GraphicsContext gc = this.getGraphicsContext2D();

        float xLoBorder = -Config.shiftBoxX * Config.monitorCellSizeX / zoom;
        float xHiBorder = (MAX_BOX_X - Config.shiftBoxX) * Config.monitorCellSizeX / zoom;
        float yLoBorder = -Config.shiftBoxY * Config.monitorCellSizeY / zoom;
        float yHiBorder = (MAX_BOX_Y - Config.shiftBoxY) * Config.monitorCellSizeY / zoom;

        for (Number[] point : frame.timeframe.getPoints()) {
            if (point[1].floatValue() < xLoBorder || xHiBorder < point[1].floatValue()) {
                continue;
            } else if (point[2].floatValue() < yLoBorder || yHiBorder < point[2].floatValue()) {
                continue;
            }
            Color color;
            try {
                int cellId = point[3].intValue();
                if (!frame.timeframe.getGrid().getCells().containsKey(cellId)) {
                    color = Color.BLACK;
                } else {
                    color = getColorForType(frame, cellId, colorizeType);
                }
            } catch (Exception ignore) {
                color = Color.YELLOW;
            }
            if (color != null) {
                gc.setFill(color);
                gc.fillOval(Config.shiftBoxX + point[1].floatValue() * Config.multiplayer, Config.shiftBoxY + point[2].floatValue() * Config.multiplayer, 1, 1);
            }
        }
    }

    protected void colorizeCells(FrameGenerator.Frame frame, ColorizeType colorizeType) {
        GraphicsContext gc = this.getGraphicsContext2D();

        float xLoBorder = -Config.shiftBoxX * Config.monitorCellSizeX / zoom;
        float xHiBorder = (MAX_BOX_X - Config.shiftBoxX) * Config.monitorCellSizeX / zoom;
        float yLoBorder = -Config.shiftBoxY * Config.monitorCellSizeY / zoom;
        float yHiBorder = (MAX_BOX_Y - Config.shiftBoxY) * Config.monitorCellSizeY / zoom;

        for (Integer cellId : frame.timeframe.getGrid().getCells().keySet()) {
            Parser.GridCell gridCell = FrameGenerator.gridSchema.get(cellId);
            if (gridCell.xLo < xLoBorder || gridCell.xHi < xLoBorder
                    || xHiBorder < gridCell.xLo || xHiBorder < gridCell.xHi) {
                continue;
            } else if (gridCell.yLo < yLoBorder || gridCell.yHi < yLoBorder
                    || yHiBorder < gridCell.yLo || yHiBorder < gridCell.yHi) {
                continue;
            }
            Color color = getColorForType(frame, cellId, colorizeType);
            if (color != null) {
                gc.setFill(color);
                gc.fillRect(Config.shiftBoxX + gridCell.xLo * Config.multiplayer,
                        Config.shiftBoxY + gridCell.yLo * Config.multiplayer,
                        (gridCell.xHi - gridCell.xLo) * Config.multiplayer,
                        (gridCell.yHi - gridCell.yLo) * Config.multiplayer);
            }
        }
    }

    public float getZoom() {
        return zoom;
    }

    public void changeZoom(float delta) {
        this.zoom += delta;
        if (this.zoom < 1) {
            this.zoom = 1;
        } else if (this.zoom > 100) {
            this.zoom = 100;
        }
    }

    @FXML
    protected void onZooming(ScrollEvent event) {
        int denominator = this.getZoom() > 50 ? 50 : this.getZoom() > 10 ? 100 : 200;
        this.changeZoom((float) event.getDeltaY() / denominator);
        if (this.getZoom() == 1) {
            Config.multiplayer = Config.defaultMultiplayer;
            Config.mainBoxX = Config.defaultBoxX;
            Config.mainBoxY = Config.defaultBoxY;
        } else {
            Config.multiplayer = (int) (Config.defaultMultiplayer * this.getZoom());
            Config.mainBoxX = (int) (Config.defaultBoxX * this.getZoom());
            Config.mainBoxY = (int) (Config.defaultBoxY * this.getZoom());
        }

        Config.shiftBoxX -= (int) event.getDeltaY();
        if (Config.shiftBoxX > 0) {
            Config.shiftBoxX = 0;
        }

        if (Config.mainBoxX + Config.shiftBoxX > MAX_BOX_X) {
            Config.mainBoxX = MAX_BOX_X - Config.shiftBoxX;
        }

        Config.shiftBoxY = (MAX_BOX_Y - Config.mainBoxY) / 2;

        EventBusFactory.getEventBus().post(new DrawEvent(0));
    }

    EventHandler<MouseEvent> canvasOnMousePressedEventHandler = mouseEvent -> {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            animatedCanvasX = mouseEvent.getSceneX();
            animatedCanvasY = mouseEvent.getSceneY();
            originalShiftX = Config.shiftBoxX;
            originalShiftY = Config.shiftBoxY;
        } else if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            showCoordsForRightButton(mouseEvent);
        }
    };

    EventHandler<MouseEvent> canvasOnMouseReleasedEventHandler = mouseEvent -> {
        if (mouseEvent.getButton() == MouseButton.SECONDARY) {
            EventBusFactory.getEventBus().post(new DrawEvent(0));
        }
    };

    EventHandler<MouseEvent> canvasOnMouseDraggedEventHandler = mouseEvent -> {
        if (mouseEvent.getButton() == MouseButton.PRIMARY) {
            double offsetX = mouseEvent.getSceneX() - animatedCanvasX;
            double offsetY = mouseEvent.getSceneY() - animatedCanvasY;
            Config.shiftBoxX = originalShiftX + (int) offsetX;
            Config.shiftBoxY = originalShiftY + (int) offsetY;
            EventBusFactory.getEventBus().post(new DrawEvent(0));
        }
    };

    private void showCoordsForRightButton(MouseEvent mouseEvent) {
        double canvasX = mouseEvent.getX();
        double canvasY = mouseEvent.getY();

        int surfX = (int) ((canvasX - Config.shiftBoxX) / Config.multiplayer * 1000) / 5 * 5;
        int surfX2 = (int) ((float) surfX / Config.spartaCellSize / 1000);
        int surfX1 = (int) ((canvasX - Config.shiftBoxX) / Config.multiplayer * 1000) / 5 * 5 + 5;
        float surfY = (float) ((canvasY - Config.shiftBoxY) / Config.multiplayer * 1000) / 5 * 5 / 1000;
        int surfY2 = (int) (surfY / Config.spartaCellSize);
        FrameGenerator.Frame frame = FrameGenerator.getFrameGenerator().getFrame(0);

        int countCells = 0;
        int countCellsWithValue = 0;
        float cellValue = 0f;
        float cellSumValue = 0f;
        if (FrameGenerator.inSurfSchema.containsKey(surfX)) {
            HashMap<Integer, Parser.GridCell> cellIds = FrameGenerator.inSurfSchema.get(surfX);
            Map<Integer, float[]> frameCells = frame.timeframe.getGrid().getCells();

            countCells = cellIds.size();
            for (Parser.GridCell gridCell : cellIds.values()) {
                if (frameCells.containsKey(gridCell.cellId)) {
                    if (curColorizeType == ColorizeType.DENSITY_STATIC) {
                        if (frameCells.get(gridCell.cellId)[0] > 0f) {
                            cellSumValue += frameCells.get(gridCell.cellId)[0];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[0];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.TEMPERATURE) {
                        if (frameCells.get(gridCell.cellId)[1] > 0f) {
                            cellSumValue += frameCells.get(gridCell.cellId)[1];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[1];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.VELOCITY) {
                        if (frameCells.get(gridCell.cellId)[2] > 0f) {
                            cellSumValue += frameCells.get(gridCell.cellId)[2];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[2];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.SOUND_VELOCITY) {
                        if (frameCells.get(gridCell.cellId)[3] > 0f) {
                            cellSumValue += frameCells.get(gridCell.cellId)[3];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[3];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.MACH) {
                        if (frameCells.get(gridCell.cellId)[4] < Float.MAX_VALUE) {
                            cellSumValue += frameCells.get(gridCell.cellId)[4];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[4];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.N_COUNT) {
                        if (frameCells.get(gridCell.cellId)[5] < Float.MAX_VALUE) {
                            cellSumValue += frameCells.get(gridCell.cellId)[5];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[5];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.NRHO) {
                        if (frameCells.get(gridCell.cellId)[6] < Float.MAX_VALUE) {
                            cellSumValue += frameCells.get(gridCell.cellId)[6];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[6];
                            }
                        }
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC) {
                        if (frameCells.get(gridCell.cellId)[7] > 0f) {
                            cellSumValue += frameCells.get(gridCell.cellId)[7];
                            countCellsWithValue++;
                            if (gridCell.yLo <= surfY && gridCell.yHi >= surfY) {
                                cellValue = frameCells.get(gridCell.cellId)[7];
                            }
                        }
                    }
                }
            }
        } else {
            if (FrameGenerator.gridSchemaRevert.containsKey(surfX2) &&
                    FrameGenerator.gridSchemaRevert.get(surfX2).containsKey(surfY2)) {
                int cellId = FrameGenerator.gridSchemaRevert.get(surfX2).get(surfY2);
                Map<Integer, float[]> frameCells = frame.timeframe.getGrid().getCells();

                if (frameCells.containsKey(cellId)) {
                    if (curColorizeType == ColorizeType.DENSITY_STATIC) {
                        if (frameCells.get(cellId)[0] > 0f) {
                            cellValue = frameCells.get(cellId)[0];
                        }
                    } else if (curColorizeType == ColorizeType.TEMPERATURE) {
                        if (frameCells.get(cellId)[1] > 0f) {
                            cellValue = frameCells.get(cellId)[1];
                        }
                    } else if (curColorizeType == ColorizeType.VELOCITY) {
                        if (frameCells.get(cellId)[2] > 0f) {
                            cellValue = frameCells.get(cellId)[2];
                        }
                    } else if (curColorizeType == ColorizeType.SOUND_VELOCITY) {
                        if (frameCells.get(cellId)[3] > 0f) {
                            cellValue = frameCells.get(cellId)[3];
                        }
                    } else if (curColorizeType == ColorizeType.MACH) {
                        if (frameCells.get(cellId)[4] < Float.MAX_VALUE) {
                            cellValue = frameCells.get(cellId)[4];
                        }
                    } else if (curColorizeType == ColorizeType.N_COUNT) {
                        if (frameCells.get(cellId)[5] < Float.MAX_VALUE) {
                            cellValue = frameCells.get(cellId)[5];
                        }
                    } else if (curColorizeType == ColorizeType.NRHO) {
                        if (frameCells.get(cellId)[6] < Float.MAX_VALUE) {
                            cellValue = frameCells.get(cellId)[6];
                        }
                    } else if (curColorizeType == ColorizeType.DENSITY_STATIC_DIF) {
                        cellValue = getDiffByDulov(frame, cellId);
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC_DIF) {
                        cellValue = getDiffByDulov(frame, cellId);
                    } else if (curColorizeType == ColorizeType.NRHO_DIF) {
                        cellValue = getDiffByDulov(frame, cellId);
                    } else if (curColorizeType == ColorizeType.NRHO_DULOV) {
                        cellValue = getDulovData(cellId);
                    } else if (curColorizeType == ColorizeType.DENSITY_DYNAMIC) {
                        if (frameCells.get(cellId)[7] > 0f) {
                            cellValue = frameCells.get(cellId)[7];
                        }
                    }
                }
            }
        }

        countCells = countCells == 0 ? 1 : countCells;
        countCellsWithValue = countCellsWithValue == 0 ? 1 : countCellsWithValue;
        String formattedCellValue = cellValue > 100000F ?
                String.format("%.2e", cellValue) :
                String.format("%.1f", cellValue);
        String formattedCellSumValue = (cellSumValue / countCells) > 100000F ?
                String.format("%.2e", cellSumValue / countCells) :
                String.format("%.1f", cellSumValue / countCells);
        String formattedPerCellSumValue = (cellSumValue / countCellsWithValue) > 100000F ?
                String.format("%.2e", cellSumValue / countCellsWithValue) :
                String.format("%.1f", cellSumValue / countCellsWithValue);
        String text = String.format("%.3f - %.3f см: %s | %s | %s %s",
                (float) surfX / 1000,
                (float) surfX1 / 1000,
                formattedCellValue,
                formattedCellSumValue,
                formattedPerCellSumValue,
                curColorizeType.getUnits());

        GraphicsContext gc = this.getGraphicsContext2D();

        gc.setFill(Color.GRAY);
        gc.fillRect(canvasX, 0, 1, this.getHeight());
        gc.fillRect(canvasX - ((float) text.length() * 6 / 2) - 1.5, 0, text.length() * 6 + 3, 22);
        gc.setFill(Color.WHITE);
        gc.fillText(text, canvasX - ((float) text.length() * 5 / 2), 15);

        EventBusFactory.getEventBus().post(new DrawDensityEvent((float) surfX / 1000));
    }
}
