package colorPalette;

import java.awt.image.BufferedImage;

/**
 *  
 * <div class="en">interface for comparing different color extracting algorithms.</div>
 * <div class="zh">比较不同算法的颜色提取器的接口</div>
 * @author Caltha
 *
 */
public interface ColorExtractor {

	/**
	 * <div class="en">retrieve main color in RGB</div>
	 * <div class="zh">提取主要的RGB颜色</div>
	 * @param img
	 * @param maxColors <div class="en">the number of output colors.
	 * if not enough colors are extracted, the rgb array will be filled with {-1, -1, -1}</div>
	 * <div class="zh">返回的颜色个数，如果没有足够颜色数组用{-1, -1, -1}填充</div> 
	 * 	
	 * @return {@code int[maxColors][3]} 
	 */
	public int[][] getRGBPalette(BufferedImage img, int maxColors);
	
	/**
	 * <div class="en">get ready for another img</div>
	 * <div class="zh">准备好做下一次提取</div>
	 */
	public void clear();
}
