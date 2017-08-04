package colorPalette;

import java.awt.image.BufferedImage;

/**
 * interface for comparing different color extracting algorithms. 
 * @author Caltha
 *
 */
public interface ColorExtractor {
	public int[][] getRGBPalette(BufferedImage img, int maxColors);
	public void clear();
}
