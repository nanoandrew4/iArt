package iart.color_schemes.rainbow_scheme;

import iart.color_schemes.ColorScheme;
import iart.draw.DrawEvent;
import javafx.scene.paint.Color;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Rainbow color scheme. Used when determining the color with which to draw various objects on screen.
 */
public class RainbowScheme implements ColorScheme {
	private static ArrayList<Color> colors = new ArrayList<>();
	private static long start = System.currentTimeMillis();

	/*
	 * Load all RGB colors from red to blue, for fast use in getColor().
	 */
	public RainbowScheme() {
		for (int g = 0; g < 256; g++) colors.add(Color.rgb(255, g, 0));
		for (int b = 0; b < 256; b++) colors.add(Color.rgb(0, 255, b));
		for (int r = 0; r < 256; r++) colors.add(Color.rgb(r, 0, 255));
	}

	@Override
	public void registerSuperScheme() {
		ColorScheme.superSchemes.putIfAbsent("Rainbow",
											 new ArrayList<>(Collections.singleton("rainbow_scheme.Rainbow")));
	}

	@Override
	public Color getColor(DrawEvent drawEvent, Point2D eventLoc) {
		switch (drawEvent) {
			case MOUSE_MOVE:
				return colors.get((int) (System.currentTimeMillis() - start) % colors.size());
			case KEYSTROKE:
			case LMOUSE_PRESS:
				int r = (int) (Math.random() * 255);
				int g = (int) (Math.random() * 255);
				int b = (int) (Math.random() * 255);
				return Color.rgb(r, g, b, (r + g + b) / (3d * 255d));
			case MOVE_OUTER_CIRCLE:
				return Color.WHITE;
			case MOVE_INNER_CIRCLE:
			case BACKGROUND:
				return Color.BLACK;
			default:
				return Color.BLACK;
		}
	}

	@Override
	public void startColorScheme() {
	}

	@Override
	public void stopColorScheme() {
	}
}
