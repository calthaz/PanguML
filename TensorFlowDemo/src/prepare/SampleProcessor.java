package prepare;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;

import zym.tensorflow.tools.LabelGenerator;
import zym.tensorflow.tools.SampleHelper;
import zym.tensorflow.tools.TFUtils;

public abstract class SampleProcessor {
	public static int MAX_PIC_SIZE = 128;
	private SampleProcessor(){
		
	}
	public static void main(String[] args) {
		//to save training time and repository space
		//we first compress the images and make a copy
		/*SampleHelper.batchEditImages(
				"training-materials/raw-data", 
				"training-materials", "scale-", new SampleHelper.ImgProcessor() {
					@Override
					public BufferedImage process(BufferedImage img) {
						
						BufferedImage st = null;
						if(img.getWidth()<=MAX_PIC_SIZE&&img.getHeight()<=MAX_PIC_SIZE){
							st = img;
						}else{
							Dimension des = TFUtils.scaleUniformFit(img.getWidth(), img.getHeight(), MAX_PIC_SIZE, MAX_PIC_SIZE);
							st = TFUtils.getScaledImage(img, des.width, des.height);
						}
						return st;
						
					}
				});*/
		
		//in training-materials I rename "raw-data" to "ready"
		SampleHelper.generateTrainAndEvalSets("training-materials/ready", 0.27);
		//you may see that some images can't be read in the process
		//we just have to skip them because we still have enough data
		
		//Now we can generate the label files!
		LabelGenerator lg = new LabelGenerator(new File("training-materials/ready"), new File("training-materials/ready"), 2, true);
		//you can feed an ArrayList with the labels in any order you like into this LabelGenerator, 
		//as long as the labels are strings such as "bed\hammock" or "flower\daisy" that correspond to real dir structure
		//but now I have to use the default order
		lg.run();
		//now we have the label files!
		
	}

}
