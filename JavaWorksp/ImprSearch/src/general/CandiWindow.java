package general;

import java.awt.image.BufferedImage;
import java.awt.image.RasterFormatException;
import java.io.IOException;

public class CandiWindow {
	//public BufferedImage clip;
	//private String id;
	private int w,h;
	private int x1, x2, y1, y2;//coordinates in origin picture
	public double score;
	/**
	 * 
	 * @param w draft width
	 * @param h draft height
	 * @param x1 in candidate 
	 * @param y1 in candidate 
	 * @param x2 in candidate (on border!)
	 * @param y2 in candidate (on border!)
	 */
	public CandiWindow( int w, int h, int x1, int y1, int x2, int y2) {//String id,
		super();
		if(x2<x1||y2<y1) throw new IllegalArgumentException(String.format("illegal clip size: p1(%s,%s), p2(%s,%s)", x1,y1,x2,y2));
		//this.id=id;
		this.w = w;
		this.h = h;
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;//always keep scale
	}
	
	public BufferedImage getClip(BufferedImage source) throws IOException{
		BufferedImage clip = new BufferedImage(w,h,Utils.IMAGE_TYPE);
		try{
			BufferedImage temp=source.getSubimage(x1, y1, x2-x1, y2-y1);//Since x2,y2 is on the border.....
			clip.getGraphics().drawImage(temp, 0, 0, w,h,null);
			return clip;
		}catch(RasterFormatException e){
			e.printStackTrace();
			System.out.println(String.format("Dimensions of the source: %s * %s; Clip origin: (%s,%s), Clip end point: (%s,%s)", 
					source.getWidth(), source.getHeight(),x1,y1,x2,y2));
		
			System.in.read();
		}
		//BufferedImage temp=source.getSubimage(x1, y1, x2-x1, y2-y1);
		//clip.getGraphics().drawImage(temp, 0, 0, w,h,null);
		return null;
	}
	
	/**
	 * side length divided by DRAFT_WIDTH
	 * @return (x2-x1)/w
	 */
	public double getAmplificationRatio(){
		return (double)(x2-x1)/w;
	}
	
	public int startX(){
		return x1;
	}
	
	public int startY(){
		return y1;
	}
	
	public int endX(){
		return x2;
	}
	
	public int endY(){
		return y2;
	}
	
	
}
