public class Counter {
    public static Counter instance = new Counter();

    private int sumCounter;
    private int productCounter;

    private Counter() {
        this.sumCounter = 0;
        this.productCounter = 0;
    }

    public void incrementSumCounter() {
        sumCounter++;
    }

    public void incrementProductCounter() {
        productCounter++;
    }

    public int getSumCounter() {
        return sumCounter;
    }

    public int getProductCounter() {
        return productCounter;
    }

    public void reset() {
        sumCounter = 0;
        productCounter = 0;
    }

}
