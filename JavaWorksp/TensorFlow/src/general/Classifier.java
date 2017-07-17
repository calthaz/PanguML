package general;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import tools.LabelGenerator;

public abstract class Classifier {
	public static final String RESULT_FILE_NAME = "tf-inference-results.txt";
	/**
	 * Execute Graph by Batch: 
	 * self-evident
	 * @param files list of paths to unchecked files
	 * @param BATCH_SIZE
	 * @param wr writer to write the result file
	 * @param gd Graph with batch size = 1
	 * @param gd1 Graph with batch size = BATCH_SIZE
	 */
	protected static void executeGraphByBatch(ArrayList<String> files, int BATCH_SIZE, PrintWriter wr, GraphDriver gd, GraphDriver gd1) {
		BufferedImage[] subset = null;
		int[] subsetIndex = null;
		int count=0;
		for(int i=0; i<files.size(); i++){
			if(count%BATCH_SIZE==0){
				subset=new BufferedImage[BATCH_SIZE];
				subsetIndex = new int[BATCH_SIZE];
				System.out.println("batch start: "+i);
			}
			File f = new File(files.get(i));
			try{
				//reads every image in case it were a bad one 
				BufferedImage img = ImageIO.read(f);
				if(img!= null&&img.getHeight()!=0){
					subset[count%BATCH_SIZE]=img;
					subsetIndex[count%BATCH_SIZE]=i;
					count++;
				}else{
					System.out.println("Probably not an image: "+f.getPath());
				}
			}catch  (IOException e){
				System.out.println("Error while reading "+f.getPath());
			}					

			if(count%BATCH_SIZE==0){
				//end of a batch
				String[] results = gd1.inferAndGetScore(subset);
				for(int j=0; j<BATCH_SIZE; j++){
					//wr.println(subset[j]+LABEL_SEP+results[j]);
					wr.println(files.get(subsetIndex[j])+LabelGenerator.LABEL_SEP+results[j]);
				}
				System.out.println("batch ends: "+i);
				subset = null;
				subsetIndex = null;
			}
  	    }
		if(subset!=null){
			for(int j=0; j<BATCH_SIZE; j++){
				if(subset[j]!=null){
					String result = gd.inferAndGetScore(subset[j]);
					wr.println(files.get(subsetIndex[j])+LabelGenerator.LABEL_SEP+result);
				}
			}
		}


	}
}
