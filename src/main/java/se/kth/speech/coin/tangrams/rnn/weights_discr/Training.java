package se.kth.speech.coin.tangrams.rnn.weights_discr;

import java.io.File;
import java.io.PrintWriter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.datavec.api.records.reader.SequenceRecordReader;
import org.datavec.api.records.reader.impl.csv.CSVSequenceRecordReader;
import org.datavec.api.split.NumberedFileInputSplit;
import org.deeplearning4j.datasets.datavec.SequenceRecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.GravesBidirectionalLSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.lossfunctions.LossFunctions.LossFunction;

public class Training {

	public static void main(String[] args) throws Exception {
		run(2478);
	}
	
	public static void run(int nFiles) throws Exception {

		int lstmLayerSize = 60;		//Number of units in each GravesLSTM layer
		int miniBatchSize = 32;		//Size of mini batch to use when  training (32)
		double learningRate = 0.01; //0.1
		int numEpochs = 1000;		//Total number of training epochs

		SequenceRecordReader featureReader = new CSVSequenceRecordReader(0, ";");
		SequenceRecordReader labelReader = new CSVSequenceRecordReader(0, ";");

		featureReader.initialize(new NumberedFileInputSplit(new File(MakeFeatures.featDir, "%df.csv").getAbsolutePath(), 1, nFiles));
		labelReader.initialize(new NumberedFileInputSplit(new File(MakeFeatures.featDir, "%dl.csv").getAbsolutePath(), 1, nFiles));

		DataSetIterator iter = new SequenceRecordReaderDataSetIterator(featureReader, labelReader, miniBatchSize, -1, true, SequenceRecordReaderDataSetIterator.AlignmentMode.ALIGN_END);
		
		int nOut = iter.totalOutcomes();
		System.out.println("Total outcomes: " + nOut);

		//Set up network configuration:
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).iterations(1)
				.learningRate(learningRate)
				.rmsDecay(0.95)
				.seed(12345)
				.regularization(true)
				.l2(0.001)
				.dropOut(0.5)
				.weightInit(WeightInit.XAVIER)
				.updater(Updater.RMSPROP) // RMSPROP, ADAGRAD, ADAM
				.list()
				// Input layer
				.layer(0, new GravesBidirectionalLSTM.Builder().nIn(iter.inputColumns()).nOut(lstmLayerSize)
						.activation(Activation.TANH).build())
				// Hidden layer
				//.layer(1, new GravesBidirectionalLSTM.Builder().nIn(lstmLayerSize).nOut(lstmLayerSize)
				//		.activation(Activation.TANH).build())
				.layer(1, new RnnOutputLayer.Builder(LossFunction.MSE).activation(Activation.SIGMOID)      
						.nIn(lstmLayerSize).nOut(nOut).build())
				//.backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(tbpttLength).tBPTTBackwardLength(tbpttLength)
				.pretrain(false).backprop(true)
				.build();
		
		
		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
		//net.setListeners(new ScoreIterationListener(1), new MyListener());

		//Print the  number of parameters in the network (and for each layer)
		Layer[] layers = net.getLayers();
		int totalNumParams = 0;
		for( int i=0; i<layers.length; i++ ){
			int nParams = layers[i].numParams();
			System.out.println("Number of parameters in layer " + i + ": " + nParams);
			totalNumParams += nParams;
		}
		System.out.println("Total number of network parameters: " + totalNumParams);

		PrintWriter trainingLog = new PrintWriter(MakeFeatures.modelDir + "/training.txt");
		
		int epoch;
		for(epoch = 1; epoch <= numEpochs; epoch++ ){
			System.out.println("Epoch: " + epoch);
			
			Mean mean = new Mean();
			while(iter.hasNext()){
				DataSet ds = iter.next();
				net.fit(ds);
				mean.increment(net.score());
				//System.out.println("Iteration score: " + net.score());
			}
			System.out.println("Epoch score: " + mean.getResult());
			trainingLog.println(epoch + " " + mean.getResult());
			trainingLog.flush();
					//  if (epoch > 10 && lastScore - score < 0.05 && lastScore > score)
			// 	break;
			iter.reset();
			
			if (epoch % 10 == 0)
				ModelSerializer.writeModel(net, new File(MakeFeatures.modelDir + "/model-" + epoch + ".net"), true);

		}
		trainingLog.close();

		System.out.println("\n\nTraining complete after " + epoch + " epochs");
	}
}
