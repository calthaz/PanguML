package colorPalette;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * <div class="en">test ColorPalette and algorithms to extract colors</div>
 * <div class="zh">测试ColorPalette和颜色提取算法</div>
 *
 */
public class paletteDriver {
	/**
	 * <span class="en">palette that contains all colors</span>
	 * <span class="zh">有所有颜色的色卡</span>
	 */
	public static final String FULL_PAL = "palettes/md-full.palette";
	/**
	 * <span class="en">palette that contains bright colors</span>
	 * <span class="zh">有所有明亮颜色的色卡</span>
	 */
	public static final String BRIGHT_PAL = "palettes/md-original-pure.palette";
	/**
	 * <span class="en">palette that contains colors selected from desaturated colors</span>
	 * <span class="zh">有所有降低饱和度的颜色的色卡</span>
	 */
	public static final String DES_PAL = "palettes/md-desaturate-pure.palette";
	
	
	private static void printColorBlocks(int[][] colors){
		for(int[] i : colors){
				System.out.println(
					String.format("<div class=\"swatch\" style=\"width=65px; height=50px; background-color: rgb(%d, %d, %d)\"></div>", 
							i[0], i[1], i[2]));
		}
	}
	
	/**
	 * <div class="zh">进行测试</div><div class="en">run the test</div>
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			//BufferedImage img = ImageIO.read(new File("F:/tmp/styles/mediterranean/bedroom/pipi059126993.png"));
			//BufferedImage img = ImageIO.read(new File("F:/tmp/styles/mediterranean/bedroom/pipi67132787.png"));
			BufferedImage img = ImageIO.read(new File("F:/TensorFlowDev/JavaWorksp/TensorFlow/img/rainbow.jpg"));
			int[][] colors = ColorThief.getPalette(img, 6);
			printColorBlocks(colors);
			OctTree ot = new OctTree();
			System.out.println("<div>-----------------Octree palette------------</div>");
			colors = ot.getRGBPalette(img, 10);
			printColorBlocks(colors);
			/*
			for(int[] i : colors){
				int c = i[0]<<16|i[1]<<8|i[2];
				String s = Integer.toHexString(c);
				while(s.length()<6){
					s = "0"+s;
				}
				//System.out.println("#"+s);
				System.out.println(
						String.format("<div class=\"swatch\" style=\"width=65px; height=50px; background-color: #%s\"></div>", 
								s));
			}
			for(int[] i : colors){
				int c = i[0]<<16|i[1]<<8|i[2];
				System.out.println("#"+Integer.toHexString(c));(,
			}*/
			System.out.println("<div>-----------------Octree with extended material design palette------------</div>");
			ColorPaletteReader rd = new ColorPaletteReader(BRIGHT_PAL);
			colors = ot.getPaletteAccordingTo(rd.getRGBPalette(), img, 10);
			printColorBlocks(colors);
			
			int num = 1000;
			long time = System.currentTimeMillis();
			for(int i=0; i<num; i++){
				colors = ColorThief.getPalette(img, 10);
			}
			System.out.println("Time for lok: "+ (System.currentTimeMillis()-time));
			time = System.currentTimeMillis();
			for(int i=0; i<num; i++){
				ot.clear();
				colors = ot.getPaletteAccordingTo(rd.getRGBPalette(),img, 10);
			}
			System.out.println("Time for oct: "+ (System.currentTimeMillis()-time));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
/*
580;
5400
use array: 5300
<300: 2861
<150: 730
//palette: 790
<75: 224
*/
