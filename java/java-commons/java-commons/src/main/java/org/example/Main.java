package org.example;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");

        int[] arr = {1, 2, 3, 4, 5};

        predic(arr);
        totalSales(arr);
    }

    private static void predic(int[] a) {
        a = Arrays.copyOf(a, a.length);
        a[0]++;
        a[1]++;
        System.out.println("predic :: " + (a[0] + a[1] + a[2] + a[3] + a[4]));
    }

    private static void totalSales(int[] a) {
        System.out.println("totalSales :: " + (a[0] + a[1] + a[2] + a[3] + a[4]));
    }
}