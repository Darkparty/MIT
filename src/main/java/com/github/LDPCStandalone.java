package com.github;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LDPCStandalone {
    private static final BinaryMatrix GEN_MATRIX = new BinaryMatrix(new Integer[][]
            {       {1, 0, 0, 0, 1, 1, 0, 1, 0, 1},
                    {0, 1, 0, 0, 1, 0, 0, 1, 1, 0}, // changed here at 7 pos to 0
                    {0, 0, 1, 0, 0, 0, 1, 1, 0, 1},
                    {0, 0, 0, 1, 0, 1, 1, 0, 1, 0}});
    private static final BinaryMatrix CHECK_MATRIX = new BinaryMatrix(new Integer[][]
            {       {1, 1, 0, 0, 1, 0, 0, 0, 0, 0},
                    {1, 0, 0, 1, 0, 1, 0, 0, 0, 0},
                    {0, 0, 1, 1, 0, 0, 1, 0, 0, 0},
                    {1, 1, 1, 0, 0, 0, 0, 1, 0, 0},
                    {0, 1, 0, 1, 0, 0, 0, 0, 1, 0},
                    {1, 0, 1, 0, 0, 0, 0, 0, 0, 1}});
    private static final int TIMEOUT = 100;

    public static void main(String[] args) throws Exception {
        //1. Checking matrices and generate data
        System.out.println(String.format("Hamming code for %d info and %d all bits sizes",
                GEN_MATRIX.getRows(), GEN_MATRIX.getColumns()));
        Thread.sleep(TIMEOUT);
        System.out.println("Checking that Gen matrix and check matrix follow the equality: G*H(t)=0:\n"
                + GEN_MATRIX.multiplyWithXors(CHECK_MATRIX.transposed()));
        Thread.sleep(TIMEOUT);
        System.out.println("Generating random info sequence:");
        Thread.sleep(TIMEOUT);
        BinaryMatrix infoBitsIn = new BinaryMatrix(generateBits(GEN_MATRIX.getRows()).toArray(new Integer[GEN_MATRIX.getRows()]));
        System.out.println("Generated: " + infoBitsIn);
        Thread.sleep(TIMEOUT);
        System.out.println("Generating full sequence: ");
        Thread.sleep(TIMEOUT);
        BinaryMatrix allBitsIn = infoBitsIn.multiplyWithXors(GEN_MATRIX);
        System.out.println("Full sequence: " + allBitsIn);
        Thread.sleep(TIMEOUT);

        //2.Tests
        System.out.println("----------TESTS-----------");
        for (int i = 0; i < 3; i ++) {
            System.out.println(String.format("Transferring sequence with %d errors.", i));
            Thread.sleep(TIMEOUT);
            System.out.println("Complete sequence: " + allBitsIn);
            Thread.sleep(TIMEOUT);
            ArrayList<Integer> allBitsOutWithErrorsList = transferWithErrors((ArrayList<Integer>) allBitsIn.getRowAsList(0), i);
            BinaryMatrix allBitsOutWithErrors = new BinaryMatrix(allBitsOutWithErrorsList.toArray(new Integer[allBitsOutWithErrorsList.size()]));
            System.out.println("Received sequence: " + allBitsOutWithErrors);
            Thread.sleep(TIMEOUT);
            for (int j = 1; j <= 10; j++) {
                if (allBitsOutWithErrors.hardDecodeStep(CHECK_MATRIX) == true) {
                    break;
                } else {
                    System.out.println("Corrected sequence on step " + j + ": " + allBitsOutWithErrors);
                    Thread.sleep(100);
                }
            }
            System.out.println("RESULT:");
            System.out.println("Complete sequence: " + allBitsIn);
            System.out.println("Received sequence: [" + allBitsOutWithErrorsList + "]");
            System.out.println("   Fixed sequence: " + allBitsOutWithErrors + "\n");
            Thread.sleep(TIMEOUT);
        }

    }

    private static ArrayList<Integer> generateBits(int length) {
        ArrayList<Integer> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(i, (int) (Math.round(Math.random()) % 2));
        }
        return result;
    }

    private static ArrayList<Integer> transferWithErrors(ArrayList<Integer> allBitsIn, int numberOfErrors) {
        ArrayList<Integer> result = new ArrayList<>(allBitsIn);
        if (numberOfErrors != 0) {
            ArrayList<Integer> errorsIndexes = new ArrayList<>(numberOfErrors);
            for (int i = 0; i < numberOfErrors; i++) {
                int possibleIndex = (int) Math.round(Math.random() * 1000) % allBitsIn.size();
                if (!errorsIndexes.contains(possibleIndex)) {
                    errorsIndexes.add(i, possibleIndex);
                    result.set(possibleIndex, result.get(possibleIndex).equals(1) ? 0 : 1);
                } else {
                    i--;
                }
            }
        }
        return result;
    }

    public static class BinaryMatrix<T> {
        private T[][] values;
        private int rows;
        private int columns;

        public BinaryMatrix(T[][] values) {
            this.values = values;
            this.rows = values.length;
            this.columns = values[0].length;
        }

        public BinaryMatrix(int rows, int columns, Class<T> type) {
            this.rows = rows;
            this.columns = columns;
            this.values = (T[][]) Array.newInstance(type, rows, columns);
        }

        public BinaryMatrix(T[] rowValues) {
            this.rows = 1;
            this.columns = rowValues.length;
            this.values = (T[][]) Array.newInstance(rowValues[0].getClass(), rows, columns);
            values[0] = rowValues;
        }

        public T getValue(int row, int column) {
            return values[row][column];
        }

        public void setValue(int row, int column, T value) {
            values[row][column] = value;
        }

        public T[] getRow(int row) {
            return values[row];
        }

        public List<T> getRowAsList(int row) {
            List<T> result = new ArrayList<>();
            for (T value : values[row])
                result.add(value);
            return result;
        }

        public void setRow(int row, T[] rowValues) {
            values[row] = rowValues;
        }

        public int getRows() {
            return rows;
        }

        public int getColumns() {
            return columns;
        }

        public T[][] getValues() {
            return values;
        }

        public BinaryMatrix multiplyWithXors(BinaryMatrix m2) {
            if (values instanceof Integer[][] && m2.getValues() instanceof Integer[][]) {
                int resultRows = this.getRows();
                int resultColumns = m2.getColumns();
                BinaryMatrix result = new BinaryMatrix(resultRows, resultColumns, Integer.class);
                for (int k = 0; k < resultRows; k++) {
                    for (int i = 0; i < resultColumns; i++) {
                        int xoredValue = 0;
                        for (int j = 0; j < this.getColumns(); j++) {
                            xoredValue += (Integer) this.values[k][j] *
                                    (Integer) m2.getValue(j, i);
                        }
                        result.setValue(k, i, xoredValue % 2);
                    }
                }
                return result;
            } else {
                throw new UnsupportedOperationException("multiplyWithXors don't know how to multiply this types:" + values.getClass());
            }
        }

        public BinaryMatrix transposed() {
            BinaryMatrix result = new BinaryMatrix(columns, rows, values[0][0].getClass());
            for (int i = 0; i < columns; i ++) {
                for (int j = 0; j < rows; j++) {
                    result.setValue(i, j, values[j][i]);
                }
            }
            return result;
        }

        //Iteration of hard decoding algorithm. It returns true if all syndromes are false.
        //Otherwise, it corrects bits that used in more than half of syndromes
        //NOTE: use this method only for 1xN matrix of Integer values.
        public boolean hardDecodeStep(BinaryMatrix checkMatrix) {
            //1. Check if syndromes are ok
            BinaryMatrix syndromes = this.multiplyWithXors(checkMatrix.transposed());
            if (!syndromes.contains(1)) return true;

            //2.There are non-false syndromes - counting each bit usage:
            int nonFalseSyndromesCount = 0;
            int[] bitsUsage = new int[this.columns];
            for (int syndromeNum = 0; syndromeNum < syndromes.columns; syndromeNum++) {
                //Looking for non-false syndromes
                if (syndromes.getValue(0, syndromeNum).equals(1)) {
                    nonFalseSyndromesCount++;
                    for (int checkMatrixOnes = 0; checkMatrixOnes < checkMatrix.columns; checkMatrixOnes ++) {
                        //if syndrome have 1 at checkMatrixOnes then we should count this bit usage;
                        if (checkMatrix.getValue(syndromeNum, checkMatrixOnes).equals(1)) {
                            bitsUsage[checkMatrixOnes]++;
                        }
                    }
                }
            }

            //3. Finally, if bit usage is more than in half of non-false syndromes - change it
            for (int i = 0; i < this.columns; i ++) {
                if (bitsUsage[i] * 2 > nonFalseSyndromesCount)
                    values[0][i] = (T)(values[0][i].equals(1) ? Integer.valueOf(0) : Integer.valueOf(1));
            }
            return false;
        }

        public boolean contains(T element) {
            for (int i = 0; i < rows; i++)
                for (int j = 0; j < columns; j++)
                    if (values[i][j].equals(element)) return true;
            return false;
        }

        @Override
        public String toString() {
            return Arrays.deepToString(values);
        }
    }
}
