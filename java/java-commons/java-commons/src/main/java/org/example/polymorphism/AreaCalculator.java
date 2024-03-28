package org.example.polymorphism;

public class AreaCalculator {

    //square

    public int getArea(final int side) {
        return side * side;
    }

    public int  getArea(final int length, final int width) {
        return length * width;
    }
}
