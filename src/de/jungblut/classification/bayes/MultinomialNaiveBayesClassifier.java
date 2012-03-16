package de.jungblut.classification.bayes;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import de.jungblut.math.DoubleVector;
import de.jungblut.math.DoubleVector.DoubleVectorElement;
import de.jungblut.math.dense.DenseDoubleMatrix;
import de.jungblut.math.dense.DenseDoubleVector;
import de.jungblut.math.dense.DenseIntVector;
import de.jungblut.math.sparse.SparseDoubleColumnMatrix;
import de.jungblut.nlp.Vectorizer;
import de.jungblut.util.Tuple;
import de.jungblut.util.Tuple3;

/**
 * Simple multinomial naive bayes classifier.
 * 
 * @author thomas.jungblut
 * 
 */
public final class MultinomialNaiveBayesClassifier {

  private DenseDoubleMatrix probabilityMatrix;
  private DenseDoubleVector classProbability;

  public final Tuple<DenseDoubleMatrix, DenseDoubleVector> train(
      SparseDoubleColumnMatrix documentWordCounts, DenseIntVector prediction) {

    final int numDistinctElements = prediction
        .getNumberOfDistinctElementsFast() + 1;
    probabilityMatrix = new DenseDoubleMatrix(numDistinctElements,
        documentWordCounts.getRowCount(), 1.0d);

    int[] columnIndices = documentWordCounts.columnIndices();
    int[] tokenPerClass = new int[numDistinctElements];
    int[] numDocumentsPerClass = new int[numDistinctElements];

    // init the probability with the document length = word count for each token
    for (int columnIndex : columnIndices) {
      final DoubleVector document = documentWordCounts
          .getColumnVector(columnIndex);
      final int predictedClass = prediction.get(columnIndex);
      tokenPerClass[predictedClass] += document.getLength();
      numDocumentsPerClass[predictedClass]++;
      probabilityMatrix.set(predictedClass, columnIndex, document.getLength());
    }

    // log normalize
    for (int row = 0; row < numDistinctElements; row++) {
      for (int col = 0; col < documentWordCounts.getRowCount(); col++) {
        double currentWordCount = probabilityMatrix.get(row, col);
        double logNormalized = Math.log(currentWordCount
            / (tokenPerClass[row] + documentWordCounts.getRowCount() - 1));
        probabilityMatrix.set(row, col, logNormalized);
      }
    }

    classProbability = new DenseDoubleVector(numDistinctElements);
    for (int i = 0; i < numDistinctElements; i++) {
      classProbability.set(i, (numDocumentsPerClass[i] + 1)
          / (double) columnIndices.length);
    }
    return new Tuple<DenseDoubleMatrix, DenseDoubleVector>(probabilityMatrix,
        classProbability);
  }

  /**
   * Returns the maximum likely class.
   */
  public final int classify(DoubleVector document) {
    return getProbabilityDistribution(document).maxIndex();
  }

  private double getProbabilityForClass(DoubleVector document, int classIndex) {
    double probabilitySum = 0.0d;
    Iterator<DoubleVectorElement> iterateNonZero = document.iterateNonZero();
    while (iterateNonZero.hasNext()) {
      DoubleVectorElement next = iterateNonZero.next();
      double wordCount = next.getValue();
      double probabilityOfToken = probabilityMatrix.get(classIndex,
          next.getIndex());
      probabilitySum += wordCount * probabilityOfToken;
    }
    return probabilitySum;
  }

  public final DenseDoubleVector getProbabilityDistribution(
      DoubleVector document) {
    int numClasses = classProbability.getLength();
    DenseDoubleVector distribution = new DenseDoubleVector(numClasses);
    // loop through all classes and get the max probable one
    for (int i = 0; i < numClasses; i++) {
      double probability = getProbabilityForClass(document, i);
      distribution.set(i, probability);
    }

    // now it contains the log probabilities of a class
    double maxProbability = distribution.max();
    double probabilitySum = 0.0d;
    // we normalize it back
    for (int i = 0; i < numClasses; i++) {
      double logProbability = distribution.get(i);
      double normalizedProbability = Math.exp(logProbability - maxProbability)
          * classProbability.get(i);
      distribution.set(i, normalizedProbability);
      probabilitySum += normalizedProbability;
    }

    // since the sum is sometimes not 1, we need to divide by the sum
    distribution = (DenseDoubleVector) distribution.divide(probabilitySum);

    return distribution;
  }

  public static void main(String[] args) {

    MultinomialNaiveBayesClassifier classifier = new MultinomialNaiveBayesClassifier();
    Tuple3<List<String[]>, DenseIntVector, String[]> readTwentyNewsgroups = TwentyNewsgroupReader
        .readTwentyNewsgroups(new File("files/20news-bydate/"));

    // tf-idf has superior performance over plain wordcount 91% vs 99% accuracy
    List<DoubleVector> wordFrequencyVectorize = Vectorizer
        .tfIdfVectorize(readTwentyNewsgroups.getFirst());

    // I know that there is the cutoff after 7532 between test and training set,
    // so just split there.
    List<DoubleVector> testSetVectors = wordFrequencyVectorize.subList(0, 7533);
    List<DoubleVector> inputList = wordFrequencyVectorize.subList(7533,
        wordFrequencyVectorize.size());

    DenseIntVector prediction = readTwentyNewsgroups.getSecond();
    DenseIntVector predictionTest = prediction.slice(0, 7533);
    DenseIntVector predictionInput = prediction.slice(7533,
        prediction.getLength());

    SparseDoubleColumnMatrix input = new SparseDoubleColumnMatrix(inputList);

    classifier.train(input, predictionInput);

    int truePositive = 0;
    int index = 0;
    for (DoubleVector document : testSetVectors) {
      int classify = classifier.classify(document);
      if (classify == predictionTest.get(index)) {
        truePositive++;
      }
    }

    System.out
        .println("Classified correctly: " + truePositive + " of "
            + testSetVectors.size() + ". "
            + (truePositive / (double) testSetVectors.size() * 100)
            + "% accuracy!");

  }
}