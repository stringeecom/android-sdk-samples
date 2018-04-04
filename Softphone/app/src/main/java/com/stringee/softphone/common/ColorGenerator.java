package com.stringee.softphone.common;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author amulya
 * @datetime 14 Oct 2014, 5:20 PM
 */
public class ColorGenerator {

	public static ColorGenerator DEFAULT;

	static {
		DEFAULT = create(Arrays.asList(0xff00be90, 0xfff05282, 0xff4183d1, 0xff7759b3, 0xfffeba4d));
	}

	private final List<Integer> mColors;
	private final Random mRandom;

	public static ColorGenerator create(List<Integer> colorList) {
		return new ColorGenerator(colorList);
	}

	private ColorGenerator(List<Integer> colorList) {
		mColors = colorList;
		mRandom = new Random(System.currentTimeMillis());
	}

	public int getRandomColor() {
		return mColors.get(mRandom.nextInt(mColors.size()));
	}

	public int getColorByIndex(int index) {
		// return 0xffe2e2e2;
		// return 0xff1aa79c;
		return mColors.get(index % mColors.size());
	}

	// public int getColorAvatarDefault() {
	// return 0xffe2e2e2;
	// // return mColors.get(index % mColors.size());
	// }

	public int getColorApp() {
		return 0xff008995;
	}

	public int getColor(Object key) {
		return mColors.get(Math.abs(key.hashCode()) % mColors.size());
	}
}
