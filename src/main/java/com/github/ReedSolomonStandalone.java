package com.github;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ReedSolomonStandalone {

    private static final int GALOIS_FIELD_NUM_OF_EL = 7;
    private static final int GALOIS_FIELD_PRIMITIVE = 5;

    private static final int INFO_MESSAGE_LENGTH = 2;
    private static final int NUMBER_OF_ERRORS_TO_FIND = 2;

    public static void main(String[] args) {
        GaloisField gField = new GaloisField(GALOIS_FIELD_NUM_OF_EL);
        int z = GALOIS_FIELD_PRIMITIVE;
        List<Integer> infoMessage = new ArrayList<>();
        infoMessage.add(3);
        infoMessage.add(1);
//        List<Integer> infoMessage = generateIntegers(INFO_MESSAGE_LENGTH, GALOIS_FIELD_NUM_OF_EL);
        List<Integer> fullMessage = appendWithZeroes(infoMessage, NUMBER_OF_ERRORS_TO_FIND * 2);
        List<Integer> encodedMessage = applyIDFT(fullMessage, gField, z);
        List<Integer> error = generateErrors(encodedMessage.size(), GALOIS_FIELD_NUM_OF_EL, 2);
        List<Integer> encodedMessageEr = applyError(encodedMessage, error, gField, true);
        List<Integer> decodedMessageEr = applyDFT(encodedMessageEr, gField, z);

        System.out.println("Full(info+check) message: " + fullMessage);
        System.out.println("Encoded message: " + encodedMessage);
        System.out.println("Generated error: " + error);
        System.out.println("Encoded message with error: " + encodedMessageEr);
        System.out.println("Full message with errors: " + decodedMessageEr);

        //Check and correct message
        List<Integer> decodedMessageFixed = null;
        boolean messageIsCorrect = true;
        for (int i = INFO_MESSAGE_LENGTH; i < decodedMessageEr.size(); i++)
            if (decodedMessageEr.get(i) != 0) {
                messageIsCorrect = false;
                decodedMessageFixed = decodedMessageEr;
            }
        if (!messageIsCorrect) {
            System.out.println("Message has errors. Fixing it");
            List<Integer> syndrome = getSyndrome(decodedMessageEr, INFO_MESSAGE_LENGTH);
            System.out.println(syndrome);
            List<Integer> berMesCoefs = findBerMesCoefs(syndrome, gField, NUMBER_OF_ERRORS_TO_FIND);
            System.out.println(berMesCoefs);
            List<Integer> errorCalc = getForniErrors(syndrome, berMesCoefs, gField, INFO_MESSAGE_LENGTH);
            System.out.println(errorCalc);
            List<Integer> encodedErrorCalc = applyIDFT(errorCalc, gField, z);
            System.out.println(encodedErrorCalc);
            List<Integer> encodedMessageFixed = applyError(encodedMessageEr, encodedErrorCalc, gField, false);
            decodedMessageFixed = applyDFT(encodedMessageFixed, gField, z);
        }
        System.out.println("Fixed message: " + decodedMessageFixed);
    }

    private static ArrayList<Integer> generateIntegers(int length, int max) {
        ArrayList<Integer> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(i, ThreadLocalRandom.current().nextInt(max));
        }
        return result;
    }

    private static List<Integer> appendWithZeroes(List<Integer> array, int zerosNumber) {
        ArrayList<Integer> newArray = new ArrayList<>(array);
        for (int i = 0; i < zerosNumber; i++) {
            newArray.add(array.size() + i, 0);
        }
        return newArray;
    }

    private static List<Integer> generateErrors(int length, int max, int numbOfErrors) {
        List<Integer> result = new ArrayList<>(length);
        boolean[] errorsPositions = new boolean[length];
        while (numbOfErrors > 0) {
            numbOfErrors--;
            int errorPosition;
            do {
                errorPosition = ThreadLocalRandom.current().nextInt(length);
                if (!errorsPositions[errorPosition]) {
                    errorsPositions[errorPosition] = true;
                    break;
                }
            } while (true);
        }
        for (int i = 0; i < length; i++) {
            result.add(i, (errorsPositions[i]) ? ThreadLocalRandom.current().nextInt(max - 1) + 1 : 0);
        }
        return result;
    }

    private static List<Integer> applyError(List<Integer> message, List<Integer> error, GaloisField gfield, boolean addError) {
        List<Integer> result = new ArrayList<>(message.size());
        for (int i = 0; i < message.size(); i++)
            result.add(i, (addError) ? gfield.sum(message.get(i), error.get(i)) : gfield.diff(message.get(i), error.get(i)));
        return result;
    }

    //applying formula d(i)=C(z^i), where C(x) is polynomial with 'fullMessage' as coefs
    private static List<Integer> applyIDFT(List<Integer> message, GaloisField gField, int z) {
        List<Integer> newMessage = new ArrayList<>(message.size());
        for (int i = 0; i < message.size(); i++) {
            int d = 0,
                    x = gField.power(z, i);
            for (int j = 0; j < message.size(); j++) {
                d = gField.sum(d, message.get(j) * gField.power(x, j));
            }
            newMessage.add(i, d);
        }
        return newMessage;
    }

    //applying formula C(i)=(1/N)*(SIGM(for j in 0:N-1)d(j)*z^(-i*j))
    private static List<Integer> applyDFT(List<Integer> message, GaloisField gField, int z) {
        List<Integer> newMessage = new ArrayList<>(message.size());
        for (int i = 0; i < message.size(); i++) {
            int c = 0,
                    poweredZ = gField.power(z, (-1) * i);
            for (int j = 0; j < message.size(); j++) {
                c = gField.sum(c, message.get(j) * gField.power(poweredZ, j));
            }
            newMessage.add(i, gField.div(c, message.size()));
        }
        return newMessage;
    }

    private static List<Integer> getSyndrome(List<Integer> message, int infoMessageLength) {
        List<Integer> syndrome = new ArrayList<>(message.size() - infoMessageLength);
        for (int i = infoMessageLength; i < message.size(); i++) {
            syndrome.add(i - infoMessageLength, message.get(i));
        }
        return syndrome;
    }

    private static List<Integer> findBerMesCoefs(List<Integer> syndrome, GaloisField gField, int numOfErrs) {
        List<Integer> coefs = new ArrayList<>(numOfErrs + 1);
        coefs.add(0, 1);
        switch (numOfErrs) {
            case 2:
                if (syndrome.get(0) != 0) {
                    int numer1 = gField.diff(gField.div(syndrome.get(1) * syndrome.get(2), syndrome.get(0)), syndrome.get(3)),
                            denom1 = gField.diff(syndrome.get(2), gField.div(gField.power(syndrome.get(1), 2), syndrome.get(0)));
                    coefs.add(1, gField.div(numer1, denom1));
                    coefs.add(2, gField.diff(0, gField.div((gField.sum(syndrome.get(1) * coefs.get(1), syndrome.get(2)))
                            , syndrome.get(0))));
                } else {
                    coefs.add(1, gField.diff(0, gField.div(syndrome.get(2), syndrome.get(1))));
                    coefs.add(2, gField.diff(0, gField.div(gField.sum(syndrome.get(3), syndrome.get(2) * coefs.get(1)),
                            syndrome.get(1))));
                }
                break;
            default:
                throw new UnsupportedOperationException("Method findBerMesCoefs doesn't support this number of errors: " + numOfErrs);
        }
        return coefs;
    }

    private static List<Integer> getForniErrors(List<Integer> syndrome, List<Integer> berMesCoefs, GaloisField gField, int infoMessageLength) {
        List<Integer> result = new ArrayList<>(syndrome.size() + infoMessageLength);
        //fill errorsVector with leading N zeros + syndrome, where N is infoMessageLength
        for (int i = 0; i < syndrome.size() + infoMessageLength; i++) {
            if (i < infoMessageLength)
                result.add(i, 0);
            else
                result.add(i, syndrome.get(i - infoMessageLength));
        }

        //calculating i element of fault vector by interpolation
        for (int i = infoMessageLength - 1; i >= 0; i--) {
            int sumOfOtherPoints = 0,
                    berMesCount = 0;
            for (int j = i + berMesCoefs.size() - 1; j > i; j--) {
                sumOfOtherPoints = gField.sum(sumOfOtherPoints, result.get(j) * berMesCoefs.get(berMesCount));
                berMesCount++;
            }
            result.set(i, gField.diff(0, gField.div(sumOfOtherPoints, berMesCoefs.get(berMesCount))));
        }
        return result;
    }

    public static class GaloisField {
        private final int numOfEl;

        public GaloisField(int numOfEl) {
            this.numOfEl = numOfEl;
        }

        public int power(int base, int power) {
            boolean negPow = false;
            if (power < 0) {
                negPow = true;
                power = power * (-1);
            }
            int result = 1;
            for (int i = 0; i < power; i++) {
                result = result * base % numOfEl;
            }
            return (negPow) ? div(1, result) : result;
        }

        public int mult(int a, int b) {
            return a * b % numOfEl;
        }

        public int sum(int a, int b) {
            return (a + b) % numOfEl;
        }

        public int diff(int a, int b) {
            a = a % numOfEl;
            b = b % numOfEl;
            return (a >= b) ? a - b : a - b + numOfEl;
        }

        public int div(int a, int b) {
            if (b == 0) throw new RuntimeException("Divide by zero");
            int multiplier = 2;
            do {
                multiplier++;
            } while (b * multiplier % numOfEl != 1);
            return a * multiplier % numOfEl;
        }
    }
}
