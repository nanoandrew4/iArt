package iart;

import iart.color_schemes.ColorSchemeSetup;
import iart.listeners.keyboard.KeyboardLayoutUI;
import iart.recorder.Recorder;
import iart.recorder.State;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.transform.Transform;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entry point for application. Initializes the UI portion of the program. Also initializes the keyboard and mouse
 * hooks when recording starts, and if required, initializes the keyboard layout setup process.
 */
public class Main extends Application {

	private static String dirSeparator = System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "/";
	public static String iArtFolderPath = System.getProperty("user.home") + dirSeparator + "Pictures" + dirSeparator +
										  "iArt" + dirSeparator;

	private Recorder recorder = new Recorder();

	public static double screenWidth, screenHeight;
	private double sceneWidth, sceneHeight;
	private boolean windowFocused = false;

	private Scene previewScene;
	private Group previewGroup;

	private MenuBar menuBar = new MenuBar();
	private MenuItem startRecording = new MenuItem("Start"), pauseRecording = new MenuItem("Pause"),
			stopRecording = new MenuItem("Stop");
	private ImageView geomPreview = new ImageView(); // Canvas preview

	private SnapshotParameters snapshotParameters = new SnapshotParameters();

	// Location on disk of the keyboard layout
	public static final String keysFileLoc = System.getProperty("user.home") + "/.iart_keys";

	private static Spinner<Double> resMultiplierSpinner = new Spinner<>(1d, 16d, 1d, 0.1);

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) {
		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		logger.setUseParentHandlers(false);

		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e) {
			e.printStackTrace();
		}

		// Get screen sizes, supports multiple monitors
		for (Screen s : Screen.getScreens()) {
			if (s.getBounds().getMaxX() > screenWidth)
				screenWidth = s.getBounds().getMaxX();
			if (s.getBounds().getMaxY() > screenHeight)
				screenHeight = s.getBounds().getMaxY();
		}

		sceneWidth = (int) (screenWidth * .25d);
		sceneHeight = (int) (screenHeight * .25d);

		previewScene = new Scene(previewGroup = new Group(), sceneWidth, sceneHeight);

		setupMenuBar(primaryStage);

		setStageListeners(primaryStage);
		primaryStage.setScene(previewScene);
		primaryStage.setTitle("iArt");
		primaryStage.show();

		primaryStage.setOnCloseRequest(event -> {
			if (Recorder.state == State.RECORDING) {
				Recorder.createIArtDirIfNotExists();
				recorder.saveImage(new File(iArtFolderPath + new Date().toString()));
			}
			System.exit(0);
		});

		if (!Files.exists(Paths.get(keysFileLoc)))
			new KeyboardLayoutUI(primaryStage);
	}

	/**
	 * Sets up the menu bar and the menus it includes, which allow the user to start/pause/stop recording and change
	 * color schemes.
	 *
	 * @param primaryStage Main stage, so that if the recording is stopped, a snapshot can be taken and stored
	 */
	private void setupMenuBar(Stage primaryStage) {
		menuBar.prefWidthProperty().bind(primaryStage.widthProperty());
		menuBar.setOnMouseEntered(event -> {
			if (Recorder.state == State.RECORDING)
				menuBar.setOpacity(1);
		});
		menuBar.setOnMouseExited(event -> {
			if (Recorder.state == State.RECORDING)
				menuBar.setOpacity(.2);
		});

		Menu fileMenu = new Menu("File");

		MenuItem resetKeyboardLayout = new MenuItem("Reset keyboard layout");
		resetKeyboardLayout.setOnAction(event -> new KeyboardLayoutUI(primaryStage));

		startRecording.setOnAction(event -> {
			if (recorder.startRecording(this, Math.sqrt(resMultiplierSpinner.getValue()))) {
				menuBar.setOpacity(0.5);
				previewScene.setRoot(previewGroup = new Group(geomPreview, menuBar));
				updateSnapshotParams();
				refreshPreview();
			}
		});

		pauseRecording.setOnAction(event -> recorder.pauseRecording(pauseRecording));

		stopRecording.setOnAction(event -> {
			if (recorder.stopRecording(primaryStage))
				menuBar.setOpacity(1);
		});

		fileMenu.getItems().addAll(resetKeyboardLayout, new SeparatorMenuItem(), startRecording, pauseRecording,
								   stopRecording);

		resMultiplierSpinner.setEditable(true);

		Menu resSpinnerMenu = new Menu("Resolution Multiplier", null, new CustomMenuItem(resMultiplierSpinner, false));

		// Setup menu bar
		menuBar.getMenus().addAll(fileMenu, resSpinnerMenu);
		ColorSchemeSetup.setupColorSchemes(menuBar);

		previewGroup.getChildren().addAll(menuBar);
	}

	/**
	 * Refreshes the preview window in the main stage. Called when the window is active and a shape is drawn through
	 * the Drawer class.
	 */
	public void refreshPreview() {
		if (!windowFocused || recorder.getCanvas() == null)
			return;
		try {
			geomPreview.setImage(recorder.getCanvas().snapshot(snapshotParameters, null));
		} catch (Exception e) {
			WritableImage img = Recorder.tiledNodeSnapshot(recorder.getCanvas());
			geomPreview.setImage(img);
		}
	}

	/**
	 * Sets main window resize listeners, and closing listeners.
	 *
	 * @param stage Stage to set listeners for (primaryStage)
	 */
	private void setStageListeners(Stage stage) {
		// Cleanup if window is closed, ensure all threads end
		stage.setOnCloseRequest(event -> {
			Recorder.state = State.STOPPED;
			try {
				GlobalScreen.unregisterNativeHook();
			} catch (NativeHookException e) {
				e.printStackTrace();
			}
			Platform.exit();
			System.exit(0);
		});

		// Track if window loses focus, no need to waste processing power updating the preview if not focused
		stage.focusedProperty().addListener((observable, oldValue, newValue) -> windowFocused = newValue);

		// Track if UI is resized, and update previewScene size appropriately
		previewScene.widthProperty().addListener((obs, oldVal, newVal) -> {
			sceneWidth = newVal.intValue();
			updateSnapshotParams();
			refreshPreview();
		});
		previewScene.heightProperty().addListener((obs, oldVal, newVal) -> {
			sceneHeight = newVal.intValue();
			updateSnapshotParams();
			refreshPreview();
		});
	}

	/**
	 * Updates the snapshot parameters in order to accurately render the preview of the art being drawn. The snapshot
	 * parameters take care of scaling the original image down to a size that is adequate for the preview window.
	 */
	private void updateSnapshotParams() {
		snapshotParameters.setTransform(
				Transform.scale(sceneWidth / (double) screenWidth, sceneHeight / (double) screenHeight)
		);
	}
}
