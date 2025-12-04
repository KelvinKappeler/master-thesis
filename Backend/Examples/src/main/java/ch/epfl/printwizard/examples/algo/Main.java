package ch.epfl.printwizard.examples.algo;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        int[] data = {42, 7, 13, 7, 100, -3, 0, 18, 5};

        System.out.println("Initial : " + Arrays.toString(data));
        
        quickSort(data, 0, data.length - 1);

        System.out.println("Sorted : " + Arrays.toString(data));
    }

    public static void quickSort(int[] array, int begin, int end) {
        if (begin < end) {
            int pivotIndex = partition(array, begin, end);
            
            quickSort(array, begin, pivotIndex - 1);
            quickSort(array, pivotIndex + 1, end);
        }
    }

    private static int partition(int[] array, int begin, int end) {
        int pivot = array[end];
        int i = begin - 1;

        for (int j = begin; j < end; j++) {
            if (array[j] <= pivot) {
                i++;
                
                int tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
            }
        }
        
        int tmp = array[i + 1];
        array[i + 1] = array[end];
        array[end] = tmp;

        return i + 1;
    }
}
