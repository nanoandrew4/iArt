package iart.recorder;

/**
 * Set of states the ImageRecorder can be in. They are used to control the flow of the program.
 * <p>
 * PRE_RECORDING indicates that the recording process is being set up, but the listeners should not start processing
 * input events yet, since all necessary parts may not be set up and calibrated properly at the present time.
 * <p>
 * RECORDING indicates the program is recording, which makes ImageRecorder to submit Operation objects to ImageDrawer,
 * to be drawn on to the canvas and the screen.
 * <p>
 * CALIBRATING indicates that the program is recalibrating it's hooks in response to a change in screen layout,
 * in order to preserve the integrity of the drawing process
 * <p>
 * PAUSED indicates the program is not tracking the cursor, until resumed.
 * <p>
 * STOPPED indicates the program has stopped tracking the cursor and is finalizing the image, to save it and start anew.
 */
public enum RecorderState {
	PRE_RECORDING, RECORDING, CALIBRATING, PAUSED, STOPPED;

	private static RecorderState state = STOPPED;

	public static RecorderState getState() {
		return state;
	}

	public static void setState(RecorderState state) {
		if (state == null)
			throw new IllegalStateException("RecorderState instance cannot be set to null");
		RecorderState.state = state;
	}

	public static boolean isRecording() {
		return state.equals(RECORDING);
	}

	public static boolean isPaused() {
		return state.equals(PAUSED);
	}

	public static boolean isStopped() {
		return state.equals(STOPPED);
	}

	public static boolean isCalibrating() {
		return state.equals(CALIBRATING);
	}
}