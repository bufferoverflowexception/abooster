package com.yy.sdk.abooster.test;

/**
 * Created by nls on 2022/8/14
 * Description: ReflectTest
 */
public class ReflectTest {

    private final IReflectTest aa = new FinalTest();

    public void print() {
        //System.out.println("1111112222: " + aa);
        aa.print();
    }

    public static void main(String[] args) {
        ReflectTest test = new ReflectTest();
        test.print();
    }

    class FinalTest implements IReflectTest {
        @Override
        public void print() {
            System.out.println("1111112222: ");
        }
    }
}
