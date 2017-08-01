package colorPalette;

import java.awt.image.BufferedImage;

public interface ColorExtractor {
	public int[][] getRGBPalette(BufferedImage img, int maxColors);
	public void clear();
}
