package com.tpsoft.notifyclient.utils;

import android.content.Context;
import android.graphics.Typeface;

public class CustomFontsLoader {

	public static final int FONT_NAME_ST = 0;
	public static final int FONT_NAME_KT = 1;
	public static final int FONT_NAME_FS = 2;
	public static final int FONT_NAME_HT = 3;

	private static final int NUM_OF_CUSTOM_FONTS = 4;

	private static boolean fontsLoaded = false;

	private static Typeface[] fonts = new Typeface[4];

	private static String[] fontPath = { "fonts/FONT_NAME_1.ttf",
			"fonts/FONT_NAME_2.ttf", "fonts/FONT_NAME_3.ttf",
			"fonts/FONT_NAME_4.ttf" };

	/**
	 * Returns a loaded custom font based on it's identifier.
	 * 
	 * @param context
	 *            - the current context
	 * @param fontIdentifier
	 *            = the identifier of the requested font
	 * 
	 * @return Typeface object of the requested font.
	 */
	public static Typeface getTypeface(Context context, int fontIdentifier) {
		if (!fontsLoaded) {
			loadFonts(context);
		}
		return fonts[fontIdentifier];
	}

	private static void loadFonts(Context context) {
		for (int i = 0; i < NUM_OF_CUSTOM_FONTS; i++) {
			fonts[i] = Typeface.createFromAsset(context.getAssets(),
					fontPath[i]);
		}
		fontsLoaded = true;

	}
}
