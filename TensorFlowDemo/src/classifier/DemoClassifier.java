package classifier;

import zym.tensorflow.general.Classifier;
import zym.tensorflow.general.GraphDriver;
import zym.tensorflow.tools.LabelGenerator;

public class DemoClassifier extends Classifier {
	
	private static final int PIC_SIZE = 64;
	private static final int BATCH_SIZE = 20;
	
	public DemoClassifier(){
		GraphDriver.LIB_PATH  = "tensorflow_jni.dll";
		this.modelPath = "models/train3500-1/frozen_graph.pb";
		this.batchModelPath = "models/train3500-"+BATCH_SIZE+"/frozen_graph.pb";
		this.labelPath = "training-materials/ready/eval/"+LabelGenerator.LABEL_TEXT_FILE_NAME;
		System.out.println(batchModelPath);
	}
	
	public String infer(String[] inputPaths){
		return loadAndRun(inputPaths);
	}

	@Override
	public int getImageSize() {
		return PIC_SIZE;
	}

	@Override
	public int getBatchSize() {
		return BATCH_SIZE;
	}

	@Override
	public String getNormMethod() {
		return "resize";
	}

}
