package iart;

import iart.color_schemes.ColorSchemeSetup;
import iart.listeners.keyboard.KeyboardLayoutUI;
import iart.multimonitor.MultiMonitorSettings;
import iart.recorder.Recorder;
import iart.recorder.RecorderState;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.*;
import javafx.scene.image.Image;
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
public class JFXMain extends Application {

	// Location on disk of the keyboard layout
	public static final String keysFileLoc = System.getProperty("user.home") + "/.iart_keys";
	private static final String dirSeparator = System.getProperty("os.name").toLowerCase().contains("windows") ? "\\" : "/";
	public static String iArtFolderPath = System.getProperty("user.home") + dirSeparator + "Pictures" + dirSeparator +
										  "iArt" + dirSeparator;
	private static Spinner<Double> resMultiplierSpinner;
	private final Recorder recorder = new Recorder();
	private final ImageView geomPreview = new ImageView(); // Canvas preview
	private double sceneWidth, sceneHeight;
	private boolean windowFocused = false;
	private Scene previewScene;
	private Group previewGroup;
	private MenuBar menuBar;
	private MenuItem startRecording;
	private MenuItem pauseRecording;
	private MenuItem stopRecording;
	private SnapshotParameters snapshotParameters;

	public static void main(String[] args) {
		launch(args);
	}

	public static void resetScreenDimensions() {
		GlobalVariables.screenWidth = 0;
		GlobalVariables.screenHeight = 0;
		// Get screen sizes, supports multiple monitors
		for (int s = 0; s < Screen.getScreens().size(); s++) {
			GlobalVariables.screenWidth = Math.max(GlobalVariables.screenWidth, (int) Screen.getScreens().get(s).getBounds().getMaxX());
			GlobalVariables.screenHeight = Math.max(GlobalVariables.screenHeight, (int) Screen.getScreens().get(s).getBounds().getMaxY());
		}
	}

	@Override
	public void start(Stage primaryStage) {
		resMultiplierSpinner = new Spinner<>(1d, 16d, 1d, 0.1);
		menuBar = new MenuBar();
		startRecording = new MenuItem("Start");
		pauseRecording = new MenuItem("Pause");
		stopRecording = new MenuItem("Stop");
		snapshotParameters = new SnapshotParameters();

		primaryStage.getIcons().add(new Image(JFXMain.class.getResourceAsStream("/icons/icon.png")));

		Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.OFF);
		logger.setUseParentHandlers(false);

		try {
			GlobalScreen.registerNativeHook();
		} catch (NativeHookException e) {
			e.printStackTrace();
		}

		resetScreenDimensions();

		sceneWidth = (int) (GlobalVariables.screenWidth * .4d);
		sceneHeight = (int) (GlobalVariables.screenHeight * .4d);

		previewScene = new Scene(previewGroup = new Group(), sceneWidth, sceneHeight);

		previewScene.getStylesheets().add(JFXMain.class.getResource("/styles/styles.css").toExternalForm());

		setupMenuBar(primaryStage);

		setStageListeners(primaryStage);
		primaryStage.setScene(previewScene);
		primaryStage.setTitle("iArt");
		primaryStage.show();

		setOnCloseRequestHandler(primaryStage);

		if (!Files.exists(Paths.get(keysFileLoc)))
			new KeyboardLayoutUI(primaryStage);
	}

	private void setOnCloseRequestHandler(Stage primaryStage) {
		// TODO: SHOW MESSAGE WHILE CLOSING, SO USER DOESN'T THINK APP FROZE
		primaryStage.setOnCloseRequest(event -> {
			if (RecorderState.isRecording()) {
				RecorderState.setState(RecorderState.STOPPED);
				Recorder.createIArtDirIfNotExists();
				recorder.saveImage(new File(iArtFolderPath + new Date().toString()));
			}
			System.exit(0);
		});
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
			if (RecorderState.isRecording())
				menuBar.setOpacity(1);
		});
		menuBar.setOnMouseExited(event -> {
			if (RecorderState.isRecording())
				menuBar.setOpacity(.2);
		});

		Menu fileMenu = createActionMenu(primaryStage);

		resMultiplierSpinner.setEditable(true);
		Menu resSpinnerMenu = new Menu("Resolution Multiplier", null, new CustomMenuItem(resMultiplierSpinner, false));

		Menu multiMonitorMenu = new Menu("Multi-Monitor Support");
		RadioMenuItem multiMonitorTranslateMousePos = new RadioMenuItem("Correct cursor transition");
		multiMonitorTranslateMousePos.setOnAction(actionEvent -> GlobalVariables.transformMousePosition = !GlobalVariables.transformMousePosition);
		MenuItem multiMonitorSettings = new MenuItem("Settings...");
		multiMonitorSettings.setOnAction(actionEvent -> {
			MultiMonitorSettings.showSettingsWindow(primaryStage);
		});

		multiMonitorMenu.getItems().addAll(multiMonitorTranslateMousePos, new SeparatorMenuItem(), multiMonitorSettings);

		// Setup menu bar
		menuBar.getMenus().addAll(fileMenu, resSpinnerMenu);
		ColorSchemeSetup.setupColorSchemes(menuBar);
		menuBar.getMenus().add(multiMonitorMenu);

		previewGroup.getChildren().addAll(menuBar);
	}

	private Menu createActionMenu(Stage primaryStage) {
		Menu fileMenu = new Menu("Actions");

		MenuItem resetKeyboardLayout = new MenuItem("Reset keyboard layout");
		resetKeyboardLayout.setOnAction(event -> new KeyboardLayoutUI(primaryStage));

		startRecording.setOnAction(event -> {
			startRecording.setDisable(true);
			pauseRecording.setDisable(false);
			stopRecording.setDisable(false);
			if (RecorderState.isStopped()) {
				resMultiplierSpinner.setDisable(true);
				GlobalVariables.setResMultiplier(resMultiplierSpinner.getValue());
				recorder.startRecording(this);
				menuBar.setOpacity(0.5);
				previewScene.setRoot(previewGroup = new Group(geomPreview, menuBar));
				updateSnapshotParams();
				refreshPreview();
			}
		});

		pauseRecording.setOnAction(event -> recorder.pauseRecording(pauseRecording));
		pauseRecording.setDisable(true);

		stopRecording.setOnAction(event -> {
			if (recorder.stopRecording(primaryStage)) {
				startRecording.setDisable(false);
				pauseRecording.setDisable(true);
				stopRecording.setDisable(true);
				menuBar.setOpacity(1);
				resMultiplierSpinner.setDisable(false);
			}
		});
		stopRecording.setDisable(true);

		fileMenu.getItems().addAll(resetKeyboardLayout, new SeparatorMenuItem(), startRecording, pauseRecording, stopRecording);
		return fileMenu;
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
			RecorderState.setState(RecorderState.STOPPED);
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
				Transform.scale(sceneWidth / GlobalVariables.getVirtualScreenWidth(), sceneHeight / GlobalVariables.getVirtualScreenHeight())
									   );
	}
}