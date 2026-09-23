package leafdetector;

import leafdetector.model.LeafCluster;
import leafdetector.processing.AnalysisResult;
import leafdetector.processing.ImageAnalyser;
import leafdetector.processing.MaskSettings;
import leafdetector.processing.NoiseSettings;
import leafdetector.processing.TspPathPlanner;
import javafx.animation.KeyFrame;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LeafVisionApp extends Application {
    private final ImageView originalView = imageView();
    private final ImageView maskView = imageView();
    private final Pane overlay = new Pane();
    private final Pane pathOverlay = new Pane();
    private final List<Color> selectedColours = new ArrayList<>();
    private final ListView<String> clusterList = new ListView<>();
    private final Label status = new Label("Load an image and choose leaf colours.");
    private final Label countLabel = new Label("Clusters: 0");
    private final FlowPane swatches = new FlowPane(6, 6);
    private final ColorPicker colourPicker = new ColorPicker(Color.ORANGE);
    private final ToggleButton sampleMode = new ToggleButton("Pick from image");
    private final CheckMenuItem showLabels = new CheckMenuItem("Show cluster numbers");
    private final Slider hueSlider = slider(0, 90, MaskSettings.defaults().hueTolerance());
    private final Slider saturationSlider = slider(0, 1, MaskSettings.defaults().saturationTolerance());
    private final Slider brightnessSlider = slider(0, 1, MaskSettings.defaults().brightnessTolerance());
    private final Slider rgbSlider = slider(0, 1, MaskSettings.defaults().rgbTolerance());
    private final TextField minPixels = new TextField(String.valueOf(NoiseSettings.defaults().minPixels()));
    private final TextField maxPixels = new TextField(String.valueOf(NoiseSettings.defaults().maxPixels()));
    private final CheckBox iqrFiltering = new CheckBox("IQR outlier filter");

    private Stage stage;
    private Image loadedImage;
    private AnalysisResult analysis;
    private LeafCluster selectedCluster;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        BorderPane root = new BorderPane();
        root.setTop(menuBar());
        root.setCenter(workspace());
        root.setRight(controlPanelScroller());
        root.setBottom(statusBar());

        showLabels.setSelected(true);
        showLabels.selectedProperty().addListener((ignored, oldValue, newValue) -> drawOverlay());
        addColour(Color.ORANGE);
        addColour(Color.GOLD);
        addColour(Color.CRIMSON);

        Scene scene = new Scene(root, 980, 640);
        primaryStage.setTitle("Autumn Leaves Identification");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private MenuBar menuBar() {
        MenuItem open = new MenuItem("Open Image");
        open.setOnAction(event -> loadImage());
        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(event -> stage.close());
        Menu file = new Menu("File", null, open, new SeparatorMenuItem(), exit);

        MenuItem convert = new MenuItem("Convert to Black and White");
        convert.setOnAction(event -> convertOnly());
        MenuItem recognise = new MenuItem("Recognise Leaves");
        recognise.setOnAction(event -> analyse());
        MenuItem colourAll = new MenuItem("Randomly Colour Sets");
        colourAll.setOnAction(event -> colourAllSets());
        MenuItem animate = new MenuItem("Animate Collector Path");
        animate.setOnAction(event -> animatePath());
        Menu tools = new Menu("Tools", null, convert, recognise, colourAll, animate, showLabels);

        return new MenuBar(file, tools);
    }

    private SplitPane workspace() {
        StackPane originalPane = new StackPane(originalView, pathOverlay, overlay);
        originalPane.setStyle("-fx-background-color: #20242a;");
        overlay.setMouseTransparent(true);
        pathOverlay.setMouseTransparent(true);
        originalView.addEventHandler(MouseEvent.MOUSE_CLICKED, this::handleOriginalClick);

        StackPane maskPane = new StackPane(maskView);
        maskPane.setStyle("-fx-background-color: #111;");

        TabPane tabs = new TabPane(new Tab("Original + clusters", originalPane), new Tab("Black and white / sets", maskPane));
        tabs.getTabs().forEach(tab -> tab.setClosable(false));

        SplitPane split = new SplitPane(tabs, clusterList);
        split.setDividerPositions(0.78);
        clusterList.setPrefWidth(260);
        clusterList.getSelectionModel().selectedIndexProperty().addListener((ignored, oldIndex, newIndex) -> {
            int index = newIndex.intValue();
            if (analysis != null && index >= 0 && index < analysis.clusters().size()) {
                selectedCluster = analysis.clusters().get(index);
                maskView.setImage(ImageAnalyser.colourSingleSet(analysis, selectedCluster.root()));
                status.setText(clusterStatus(selectedCluster));
            }
        });
        return split;
    }

    private ScrollPane controlPanelScroller() {
        ScrollPane scroller = new ScrollPane(controlPanel());
        scroller.setFitToWidth(true);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setPrefViewportWidth(235);
        return scroller;
    }

    private VBox controlPanel() {
        Button load = new Button("Open image");
        load.setMaxWidth(Double.MAX_VALUE);
        load.setOnAction(event -> loadImage());
        Button addColour = new Button("Add selected colour");
        addColour.setMaxWidth(Double.MAX_VALUE);
        addColour.setOnAction(event -> addColour(colourPicker.getValue()));
        Button clearColours = new Button("Clear colours");
        clearColours.setMaxWidth(Double.MAX_VALUE);
        clearColours.setOnAction(event -> {
            selectedColours.clear();
            refreshSwatches();
        });

        Button convert = new Button("Convert mask");
        convert.setMaxWidth(Double.MAX_VALUE);
        convert.setOnAction(event -> convertOnly());
        Button recognise = new Button("Recognise leaves");
        recognise.setMaxWidth(Double.MAX_VALUE);
        recognise.setOnAction(event -> analyse());
        Button colourAll = new Button("Colour all sets");
        colourAll.setMaxWidth(Double.MAX_VALUE);
        colourAll.setOnAction(event -> colourAllSets());
        Button animate = new Button("Animate path");
        animate.setMaxWidth(Double.MAX_VALUE);
        animate.setOnAction(event -> animatePath());

        minPixels.setPrefColumnCount(7);
        maxPixels.setPrefColumnCount(7);

        VBox panel = new VBox(8,
                section("Image", load, sampleMode),
                section("Leaf colours", colourPicker, addColour, clearColours, swatches),
                section("Conversion thresholds",
                        labelledSlider("Hue", hueSlider),
                        labelledSlider("Saturation", saturationSlider),
                        labelledSlider("Brightness", brightnessSlider),
                        labelledSlider("RGB", rgbSlider)),
                section("Noise controls",
                        fieldRow("Min pixels", minPixels),
                        fieldRow("Max pixels", maxPixels),
                        iqrFiltering),
                section("Actions", convert, recognise, colourAll, animate),
                countLabel);
        panel.setPrefWidth(235);
        panel.setPadding(new Insets(8));
        panel.setStyle("-fx-background-color: #f6f7f9;");
        return panel;
    }

    private HBox statusBar() {
        HBox bar = new HBox(status);
        bar.setPadding(new Insets(7, 10, 7, 10));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: #e9edf2;");
        return bar;
    }

    private VBox section(String title, javafx.scene.Node... nodes) {
        Label label = new Label(title);
        label.setStyle("-fx-font-weight: bold;");
        VBox box = new VBox(5);
        box.getChildren().add(label);
        box.getChildren().addAll(nodes);
        return box;
    }

    private HBox fieldRow(String label, TextField field) {
        Label text = new Label(label);
        HBox row = new HBox(8, text, field);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(text, Priority.ALWAYS);
        return row;
    }

    private VBox labelledSlider(String label, Slider slider) {
        Label value = new Label();
        value.textProperty().bind(Bindings.format("%s: %.2f", label, slider.valueProperty()));
        return new VBox(3, value, slider);
    }

    private void loadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an autumn leaves image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }
        loadedImage = new Image(file.toURI().toString());
        originalView.setImage(loadedImage);
        maskView.setImage(null);
        analysis = null;
        selectedCluster = null;
        overlay.getChildren().clear();
        pathOverlay.getChildren().clear();
        clusterList.getItems().clear();
        countLabel.setText("Clusters: 0");
        status.setText("Loaded " + file.getName() + ". Pick colours or use the preset autumn swatches.");
    }

    private void convertOnly() {
        try {
            requireImageAndColours();
            maskView.setImage(ImageAnalyser.createBlackWhiteOnly(loadedImage, selectedColours, maskSettings()));
            status.setText("Black-and-white mask generated.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void analyse() {
        try {
            requireImageAndColours();
            analysis = ImageAnalyser.analyse(loadedImage, selectedColours, maskSettings(), noiseSettings());
            originalView.setImage(analysis.scaledOriginal());
            maskView.setImage(analysis.blackWhiteImage());
            selectedCluster = analysis.clusters().isEmpty() ? null : analysis.clusters().get(0);
            fillClusterList();
            drawOverlay();
            countLabel.setText("Clusters: " + analysis.clusters().size());
            status.setText("Recognition complete. Click a cluster to inspect its disjoint-set size.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void colourAllSets() {
        if (analysis == null) {
            showError("Run leaf recognition first.");
            return;
        }
        maskView.setImage(ImageAnalyser.colourAllSets(analysis));
        status.setText("All disjoint sets coloured in the mask view.");
    }

    private void animatePath() {
        if (analysis == null || analysis.clusters().isEmpty()) {
            showError("Run leaf recognition before animating a path.");
            return;
        }
        LeafCluster start = selectedCluster == null ? analysis.clusters().get(0) : selectedCluster;
        List<LeafCluster> path = TspPathPlanner.nearestNeighbour(analysis.clusters(), start);
        pathOverlay.getChildren().clear();
        SequentialTransition sequence = new SequentialTransition();
        for (int i = 1; i < path.size(); i++) {
            LeafCluster previous = path.get(i - 1);
            LeafCluster current = path.get(i);
            ImageDisplay display = displayedImage();
            Point2D previousPoint = display.toView(previous.centre());
            Point2D currentPoint = display.toView(current.centre());
            Line line = new Line(previousPoint.getX(), previousPoint.getY(), currentPoint.getX(), currentPoint.getY());
            line.setStroke(Color.web("#ffd84d"));
            line.setStrokeWidth(3);
            line.setOpacity(0.0);
            pathOverlay.getChildren().add(line);
            Timeline step = new Timeline(
                    new KeyFrame(Duration.ZERO, event -> line.setOpacity(1.0)),
                    new KeyFrame(Duration.millis(Math.max(25, 5000.0 / Math.max(1, path.size()))), event -> line.setOpacity(0.72)));
            sequence.getChildren().add(step);
        }
        sequence.setOnFinished(event -> status.setText("Animated nearest-neighbour path from cluster " + start.rank() + "."));
        sequence.play();
    }

    private void handleOriginalClick(MouseEvent event) {
        if (originalView.getImage() == null) {
            return;
        }
        Point2D point = imagePoint(event);
        if (point == null) {
            return;
        }
        if (sampleMode.isSelected()) {
            PixelReader reader = originalView.getImage().getPixelReader();
            addColour(reader.getColor((int) point.getX(), (int) point.getY()));
            status.setText("Added colour sample from image.");
            return;
        }
        if (analysis == null) {
            return;
        }
        int root = ImageAnalyser.rootAt(analysis, point.getX(), point.getY());
        LeafCluster cluster = analysis.clusterByRoot().get(root);
        if (cluster != null) {
            selectedCluster = cluster;
            clusterList.getSelectionModel().select(analysis.clusters().indexOf(cluster));
            maskView.setImage(ImageAnalyser.colourSingleSet(analysis, root));
            status.setText(clusterStatus(cluster));
        }
    }

    private Point2D imagePoint(MouseEvent event) {
        Image image = originalView.getImage();
        ImageDisplay display = displayedImage();
        double x = (event.getX() - display.offsetX()) / display.scale();
        double y = (event.getY() - display.offsetY()) / display.scale();
        if (image == null || x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight()) {
            return null;
        }
        return new Point2D(x, y);
    }

    private void drawOverlay() {
        overlay.getChildren().clear();
        pathOverlay.getChildren().clear();
        if (analysis == null) {
            return;
        }
        ImageDisplay display = displayedImage();
        for (LeafCluster cluster : analysis.clusters()) {
            Rectangle rectangle = new Rectangle(
                    display.offsetX() + cluster.minX() * display.scale(),
                    display.offsetY() + cluster.minY() * display.scale(),
                    cluster.width() * display.scale(),
                    cluster.height() * display.scale());
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(Color.DODGERBLUE);
            rectangle.setStrokeWidth(1.6);
            overlay.getChildren().add(rectangle);
            if (showLabels.isSelected()) {
                Text label = new Text(
                        display.offsetX() + cluster.minX() * display.scale() + 3,
                        Math.max(12, display.offsetY() + cluster.minY() * display.scale() - 3),
                        cluster.rank() + " (" + cluster.pixelCount() + ")");
                label.setFill(Color.web("#ffe66d"));
                label.setStyle("-fx-font-weight: bold; -fx-stroke: #1f2937; -fx-stroke-width: 0.6;");
                overlay.getChildren().add(label);
            }
        }
    }

    private void fillClusterList() {
        clusterList.getItems().clear();
        for (LeafCluster cluster : analysis.clusters()) {
            clusterList.getItems().add("Cluster " + cluster.rank() + " - " + cluster.pixelCount() + " px");
        }
        if (!analysis.clusters().isEmpty()) {
            clusterList.getSelectionModel().select(0);
        }
    }

    private void addColour(Color colour) {
        selectedColours.add(colour);
        refreshSwatches();
    }

    private void refreshSwatches() {
        swatches.getChildren().clear();
        for (Color colour : selectedColours) {
            Rectangle swatch = new Rectangle(28, 22, colour);
            swatch.setStroke(Color.web("#384252"));
            swatch.setArcWidth(4);
            swatch.setArcHeight(4);
            swatches.getChildren().add(swatch);
        }
        if (selectedColours.isEmpty()) {
            swatches.getChildren().add(new Label("No colours selected"));
        }
    }

    private MaskSettings maskSettings() {
        return new MaskSettings(
                hueSlider.getValue(),
                saturationSlider.getValue(),
                brightnessSlider.getValue(),
                rgbSlider.getValue(),
                MaskSettings.defaults().maxAnalysisSize());
    }

    private NoiseSettings noiseSettings() {
        return new NoiseSettings(
                parseInt(minPixels, NoiseSettings.defaults().minPixels()),
                parseInt(maxPixels, NoiseSettings.defaults().maxPixels()),
                iqrFiltering.isSelected());
    }

    private void requireImageAndColours() {
        if (loadedImage == null) {
            throw new IllegalStateException("Choose an image first.");
        }
        if (selectedColours.isEmpty()) {
            throw new IllegalStateException("Choose at least one leaf colour.");
        }
    }

    private String clusterStatus(LeafCluster cluster) {
        return "Cluster " + cluster.rank() + ": " + cluster.pixelCount() + " pixels, bounds "
                + cluster.width() + "x" + cluster.height() + ".";
    }

    private static ImageView imageView() {
        ImageView view = new ImageView();
        view.setPreserveRatio(true);
        view.setFitWidth(640);
        view.setFitHeight(560);
        return view;
    }

    private static Slider slider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        return slider;
    }

    private static int parseInt(TextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException ex) {
            field.setText(String.valueOf(fallback));
            return fallback;
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Autumn Leaves Vision");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private ImageDisplay displayedImage() {
        Image image = originalView.getImage();
        if (image == null) {
            return new ImageDisplay(1.0, 0.0, 0.0);
        }
        double viewWidth = originalView.getBoundsInLocal().getWidth();
        double viewHeight = originalView.getBoundsInLocal().getHeight();
        double scale = Math.min(viewWidth / image.getWidth(), viewHeight / image.getHeight());
        double displayedWidth = image.getWidth() * scale;
        double displayedHeight = image.getHeight() * scale;
        return new ImageDisplay(scale, (viewWidth - displayedWidth) / 2.0, (viewHeight - displayedHeight) / 2.0);
    }

    private record ImageDisplay(double scale, double offsetX, double offsetY) {
        private Point2D toView(Point2D imagePoint) {
            return new Point2D(offsetX + imagePoint.getX() * scale, offsetY + imagePoint.getY() * scale);
        }
    }
}
