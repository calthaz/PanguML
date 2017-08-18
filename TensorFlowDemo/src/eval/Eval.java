package eval;

import java.util.ArrayList;

import classifier.DemoClassifier;
import zym.tensorflow.tools.LabelGenerator;
import zym.tensorflow.tools.TFUtils;

public class Eval {
	//
	public static void main(String[] args) {
		String[] inputPaths = {"training-materials\\ready\\eval"};
		DemoClassifier classifier = new DemoClassifier();
		classifier.infer(inputPaths);
		if(null == classifier.getResultPath()){
			//Oops, infer has failed
			System.exit(1);
		}
		
		//we can get accuracy by class as well as the total accuracy with this method
		double[] result = TFUtils.checkAccuracy(classifier.getResultPath(), classifier.getLabelPath());
		ArrayList<String> labels = LabelGenerator.readLabelsFromFile(classifier.getLabelPath());
		for(int i=0; i<labels.size(); i++){
			System.out.println(labels.get(i)+":"+result[i]);
		}
		System.out.println("total:"+result[labels.size()]);
		/*
		 * Sample output
		graph: models/train3500-20/frozen_graph.pb
		bed\round-bed:0.6845238095238095
		bed\hammock:0.711864406779661
		bed\baby-bed:0.7938144329896907
		flower\tulips:0.6150234741784038
		flower\sunflowers:0.7671957671957672
		flower\roses:0.5302013422818792
		flower\dandelion:0.6378600823045267
		flower\daisy:0.6192893401015228
		total:0.660844250363901
		
		training-materials/ready/eval/7000-tf-inference-results.txt
		takes 7000 steps to train, while
		training-materials/ready/eval/3500-tf-inference-results.txt
		only takes 3500.
		However, their accuracy are exactly the same
		Well, this is just a nasty coincidence. 
		In most cases, these two results should differ slightly. 
		But it proves that more steps on a small training set have no benefit
		
		 */
		
		//Now, if we want to get a feeling of how this model classifies images, 
		///we let it sort the pics according to its prediction
		//First, create "training-materials/result"
		TFUtils.sortByClass(classifier.getResultPath(), classifier.getLabelPath(), "training-materials/result");
		//now you can find some ridiculous results! 
	}
}
