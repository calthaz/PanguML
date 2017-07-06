package tools;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class TFUtils {
	public static double cropRate = 0.5;
	public static BufferedImage cropImage(BufferedImage img){
		int w = (int) (img.getWidth()*cropRate);
		int h = (int)(img.getHeight()*cropRate);
		int startX = (int) (Math.random()*(img.getWidth()-w));
		int startY = (int) (Math.random()*(img.getHeight()-h));
		int[] raw = new int[w*h];
		//img.getRaster().getPixels(0, 0, PIC_SIZE, PIC_SIZE, raw);java.lang.ArrayIndexOutOfBoundsException: 784
		raw = img.getRGB(startX, startY, w, h, raw, 0, w);
		BufferedImage output = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);			  
		output.setRGB(0, 0, w, h, raw, 0, w);
		return output;
	}
	/**
     * https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
     * create a new BufferedImage and and draw a scaled version of the original on the new one.
     * @param original
     * @param newWidth
     * @param newHeight
     * @return
     */
    public static BufferedImage getScaledImage(BufferedImage original, int newWidth, int newHeight){
  	  BufferedImage resized = new BufferedImage(newWidth, newHeight, original.getType());
  	  Graphics2D g = resized.createGraphics();
  	  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
  	      RenderingHints.VALUE_INTERPOLATION_BILINEAR);
  	  g.drawImage(original, 0, 0, newWidth, newHeight, 0, 0, original.getWidth(),
  	      original.getHeight(), null);
  	  g.dispose();
  	  return resized;
    }
    
    public static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
          if (probabilities[i] > probabilities[best]) {
            best = i;
          }
        }
        return best;
    }  
}
