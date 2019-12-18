package com.github;

import java.util.ArrayList;

public class HammingStandalone {
    private static final int INFO_BITS_SIZE = 4;
    private static final int CHECK_BITS_SIZE = 3;
    private static final int TIMEOUT = 100;

    public static void main(String[] args) throws Exception {
        //1. Data generation
        System.out.println(String.format("Hamming code for %d info and %d check bits",
                INFO_BITS_SIZE, CHECK_BITS_SIZE));
        Thread.sleep(TIMEOUT);
        System.out.println("Generating random sequence of bits");
        Thread.sleep(TIMEOUT);
        ArrayList<Boolean> infoBitsIn = generateBits(INFO_BITS_SIZE);
        System.out.println("Generation result: " + infoBitsIn);
        Thread.sleep(TIMEOUT);
        System.out.println("Creating check bits");
        Thread.sleep(TIMEOUT);
        ArrayList<Boolean> checkBitsIn = createCheckBits(infoBitsIn);
        ArrayList<Boolean> allBitsIn = new ArrayList<>(infoBitsIn);
        allBitsIn.addAll(INFO_BITS_SIZE, checkBitsIn);
        System.out.println("Check bits: " + checkBitsIn + "\nComplete sequence: " + allBitsIn);
        Thread.sleep(TIMEOUT);

        //2. Tests
        System.out.println("----------TESTS-----------");
        for (int i = 0; i < 3; i ++) {
            System.out.println(String.format("Transferring sequence with %d errors.", i));
            Thread.sleep(TIMEOUT);
            System.out.println("Complete sequence: " + allBitsIn);
            Thread.sleep(TIMEOUT);
            ArrayList<Boolean> allBitsOutWithErrors = transferWithErrors(allBitsIn, i);
            System.out.println("Received sequence: " + allBitsOutWithErrors);
            Thread.sleep(TIMEOUT);
            ArrayList<Boolean> allBitsOutFixed = fixBits(allBitsOutWithErrors);
            System.out.println("   Fixed sequence: " + allBitsOutFixed);
            Thread.sleep(TIMEOUT);
            System.out.println("\n");
        }
    }

    private static ArrayList<Boolean> generateBits(int length) {
        ArrayList<Boolean> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(i, Math.round(Math.random()) % 2 == 1);
        }
        return result;
    }

    private static ArrayList<Boolean> createCheckBits(ArrayList<Boolean> infoBits) {
        ArrayList<Boolean> result = new ArrayList<>(CHECK_BITS_SIZE);
        if (CHECK_BITS_SIZE == 3) {
            result.add(0, infoBits.get(0) ^ infoBits.get(1) ^ infoBits.get(2));
            result.add(1, infoBits.get(0) ^ infoBits.get(1) ^ infoBits.get(3));
            result.add(2, infoBits.get(0) ^ infoBits.get(2) ^ infoBits.get(3));
            return result;
        } else {
            throw new RuntimeException("Unsupported amount of check bits.");
        }
    }

    private static ArrayList<Boolean> transferWithErrors(ArrayList<Boolean> allBitsIn, int numberOfErrors) {
        ArrayList<Boolean> result = new ArrayList<>(allBitsIn);
        if (numberOfErrors != 0) {
            ArrayList<Integer> errorsIndexes = new ArrayList<>(numberOfErrors);
            for (int i = 0; i < numberOfErrors; i++) {
                int possibleIndex = (int) Math.round(Math.random() * 1000) % allBitsIn.size();
                if (!errorsIndexes.contains(possibleIndex)) {
                    errorsIndexes.add(i, possibleIndex);
                    result.set(possibleIndex, !result.get(possibleIndex));
                } else {
                    i--;
                }
            }
        }
        return result;
    }

    private static ArrayList<Boolean> fixBits(ArrayList<Boolean> allBitsOutWithErrors) throws Exception {
        ArrayList<Boolean> result = new ArrayList<>(allBitsOutWithErrors);
        ArrayList<Integer> controlSequence = new ArrayList<>(CHECK_BITS_SIZE);
        if (allBitsOutWithErrors.size() == INFO_BITS_SIZE + CHECK_BITS_SIZE) {
            controlSequence.add(0, (allBitsOutWithErrors.get(0) ^ allBitsOutWithErrors.get(1) ^
                    allBitsOutWithErrors.get(2) ^ allBitsOutWithErrors.get(4)) ? 1 : 0);
            controlSequence.add(1, (allBitsOutWithErrors.get(0) ^ allBitsOutWithErrors.get(1) ^
                    allBitsOutWithErrors.get(3) ^ allBitsOutWithErrors.get(5)) ? 1 : 0);
            controlSequence.add(2, (allBitsOutWithErrors.get(0) ^ allBitsOutWithErrors.get(2) ^
                    allBitsOutWithErrors.get(3) ^ allBitsOutWithErrors.get(6)) ? 1 : 0);
            int errorIndex = -1;
            switch (controlSequence.toString()) {
                case "[0, 0, 0]":
                    System.out.println("No error found!");
                    return result;
                case "[0, 0, 1]":
                    errorIndex = 6;
                    break;
                case "[0, 1, 0]":
                    errorIndex = 5;
                    break;
                case "[1, 0, 0]":
                    errorIndex = 4;
                    break;
                case "[0, 1, 1]":
                    errorIndex = 3;
                    break;
                case "[1, 1, 0]":
                    errorIndex = 1;
                    break;
                case "[1, 0, 1]":
                    errorIndex = 2;
                    break;
                case "[1, 1, 1]":
                    errorIndex = 0;
                    break;
            }
            Thread.sleep(TIMEOUT);
            System.out.println("Error at bit #" + errorIndex);
            Thread.sleep(TIMEOUT);
            result.set(errorIndex, !result.get(errorIndex));
        } else {
            throw new RuntimeException("Unsupported amount of bits while fixing.");
        }
         return result;
    }

}
