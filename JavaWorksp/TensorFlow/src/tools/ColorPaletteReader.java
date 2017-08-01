package tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

public class ColorPaletteReader {
	private ArrayList<Integer> colors;
	private static final String PALETTE_ENDING = ".palatte";
	
	public ColorPaletteReader(String path){
		colors = new ArrayList<Integer>();
		File text = new File(path);
		if(!text.exists()){
			System.out.println("Can't find palette file: "+path);
			return;
		}
		try {
			Scanner sc = new Scanner(text);
			while(sc.hasNextLine()){
				String line = sc.nextLine();
				int color = -1;
				try{
					int index=0;
					if(line.indexOf('#')!=-1){
						index = line.indexOf('#');
						color = Integer.parseInt(line.substring(index+1, index+7), 16);
					}
					if(line.indexOf("0x")!=-1){
						index = line.indexOf("0x");
						color = Integer.parseInt(line.substring(index+2, index+8), 16);
					}
				}catch(NumberFormatException e){
					System.err.println(e.getMessage());
				}
				if(color>=0&&!colors.contains(color)){
					colors.add(color);
				}
			}
			sc.close();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public ArrayList<Integer> getRawPalette(){
		return colors;
	}
	
	public ArrayList<int[]> getRGBPalette(){
		ArrayList<int[]> ret = new ArrayList<int[]>();
		for(int c : colors){
			int[] rgb = new int[3];
			rgb[0] = (c>>16)&0xFF;
			rgb[1] = (c>>8)&0xFF;
			rgb[2] = c&0xFF;
			ret.add(rgb);
		}
		return ret;
	}
	
	public String savePaletteAsSerialized(String name){
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(name+PALETTE_ENDING)));
			out.writeObject(colors);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return name+PALETTE_ENDING;
	}
	
	public String savePaletteAsText(String name){
		try {
			PrintWriter wr=new PrintWriter(name+".txt","UTF-8");
			for(int c : colors){
				String s = Integer.toHexString(c);
				while(s.length()<6){
					s = "0"+s;
				}
				wr.println("#"+s);
			}
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return name+".txt";
	}
	/*
	public String savePaletteAsLess(String name){
		try {
			PrintWriter wr=new PrintWriter(name+".less","UTF-8");
			wr.println("body{");
			for(int c : colors){
				String s = Integer.toHexString(c);
				while(s.length()<6){
					s = "0"+s;
				}
				wr.println("color: desaturate(#"+s+",50%)");
			}
			wr.println("}");
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return name+".less";
	}*/
	
	public static void main(String[] args) {
		int c = Integer.parseInt("ffffff", 16);
		System.out.println(c);
		c = Integer.parseInt("000000", 6);
		System.out.println(c);
		c = Integer.MAX_VALUE;
		System.out.println(c);
		/*
		ColorPaletteReader rd = new ColorPaletteReader("F:/TensorFlowDev/JavaWorksp/TensorFlow/palettes/md-color-palette.txt");
		ArrayList<int[]> colors = rd.getRGBPalette();
		for(int[] i : colors){
			System.out.println(
				String.format("<div class=\"swatch\" style=\"width=65px; height=50px; background-color: rgb(%d, %d, %d)\"></div>", 
						i[0], i[1], i[2]));
		}
		rd.savePaletteAsText("palettes/md-original-pure");*/
	}

}
