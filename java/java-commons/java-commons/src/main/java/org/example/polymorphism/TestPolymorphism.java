package org.example.polymorphism;

public class TestPolymorphism {

    public static void main(String[] args) {
        AreaCalculator areaCalculator = new AreaCalculator();

        System.out.printf("Area of square: %d\n", areaCalculator.getArea(5));
        System.out.printf("Area of rectangle: %d\n", areaCalculator.getArea(5, 10));
    }
}
