package zym.cv.findmaincolor;

import java.awt.image.BufferedImage;

/**
 *  
 * <span class="en">interface for comparing different color extracting algorithms.</span>
 * <span class="zh">比较不同算法的颜色提取器的接口</span>
 * @author Caltha
 *
 */
public interface ColorExtractor {

	/**
	 * <span class="en">retrieve main color in RGB</span>
	 * <span class="zh">提取主要的RGB颜色</span>
	 * @param img
	 * @param maxColors <span class="en">the number of output colors.
	 * if not enough colors are extracted, the rgb array will be filled with {-1, -1, -1}</span>
	 * <span class="zh">返回的颜色个数，如果没有足够颜色数组用{-1, -1, -1}填充</span> 
	 * 	
	 * @return {@code int[maxColors][3]} 
	 */
	public int[][] getRGBPalette(BufferedImage img, int maxColors);
	
	/**
	 * <span class="en">get ready for another img</span>
	 * <span class="zh">准备好做下一次提取</span>
	 */
	public void clear();
}
