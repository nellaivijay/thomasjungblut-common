package de.jungblut.math.minimize;

import de.jungblut.math.DoubleVector;
import de.jungblut.math.dense.DenseDoubleMatrix;
import de.jungblut.math.dense.DenseDoubleVector;

public class DenseMatrixFolder {

  /**
   * Folds the given matrices column-wise into a single vector.
   */
  public static DenseDoubleVector foldMatrices(DenseDoubleMatrix... matrices) {
    int length = 0;
    for (DenseDoubleMatrix matrix : matrices) {
      length += matrix.getRowCount() * matrix.getColumnCount();
    }

    DenseDoubleVector v = new DenseDoubleVector(length);
    int index = 0;
    for (DenseDoubleMatrix matrix : matrices) {
      for (int j = 0; j < matrix.getColumnCount(); j++) {
        for (int i = 0; i < matrix.getRowCount(); i++) {
          v.set(index++, matrix.get(i, j));
        }
      }
    }

    return v;
  }

  /**
   * Unfolds a vector into matrices by the rules defined in the sizeArray. The
   * sizeArray must have following format: in each row the row and column count
   * must be provided.<br/>
   * Example: sizeArray = {{2,3},{3,2}} will unfold into matrix 0 with 2 rows
   * and 3 columns and matrix 1 with 3 rows and 2 columns.
   */
  public static DenseDoubleMatrix[] unfoldMatrices(DoubleVector vector,
      int[][] sizeArray) {
    DenseDoubleMatrix[] arr = new DenseDoubleMatrix[sizeArray.length];
    for (int i = 0; i < sizeArray.length; i++) {
      arr[i] = new DenseDoubleMatrix(sizeArray[i][0], sizeArray[i][1]);
    }

    int currentVectorIndex = 0;
    for (int i = 0; i < arr.length; i++) {
      final int numRows = sizeArray[i][0];
      final int numColumns = sizeArray[i][1];
      for (int col = 0; col < numColumns; col++) {
        for (int row = 0; row < numRows; row++) {
          arr[i].set(row, col, vector.get(currentVectorIndex++));
        }
      }
    }

    return arr;
  }

}
