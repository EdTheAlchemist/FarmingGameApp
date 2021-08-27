package com.mobdeve.tighee.farminggameapp;

import android.util.Log;

import java.util.Random;

public abstract class Product {
    private static String TAG = "ProductTAG";

    private int productionTime, imageId, minCoins, maxCoins;
    private String name;

    public Product(String name, int productionTime, int imageId, int minCoins, int maxCoins) {
        this.name = name;
        this.productionTime = productionTime;
        this.imageId = imageId;
        this.minCoins = minCoins;
        this.maxCoins = maxCoins;
    }

    public String getName() {
        return name;
    }

    public int getProductionTime() {
        return productionTime;
    }

    public int getImageId() {
        return imageId;
    }

    public int getMinCoins() {
        return minCoins;
    }

    public int getMaxCoins() {
        return maxCoins;
    }

    /*
     * NUM TO TASK REFERENCE:
     *   0 --> CORN
     *   1 --> GRAPES
     *   2 --> APPLES
     * */
    public static Product generateProduct() {
        Random r = new Random();
        Product tempProduct = null;

        // Extract a random number to serve as the basis of which product to work
        int productType = r.nextInt(3);
        Log.d(TAG, "generateProduct: " + productType);

        switch(productType) {
            case 0:
                tempProduct = new Corn();
                break;
            case 1:
                tempProduct = new Grapes();
                break;
            case 2:
                tempProduct = new Apple();
                break;
        }

        return tempProduct;
    }

    /*
     * EXAMPLE:
     *   min -> 10; max -> 15
     *   diff -> 15-10 -> 5
     *   randInt -> from 0 to diff + 1 (or 6) -> assume rand came out to 4
     *   money -> 4 + 10 -> 14
     *   Note: rand x to y -> y is bound, so highest output is y-1
     * */
    public int generateMoney() {
        Random r = new Random();
        int difference = this.maxCoins - this.minCoins;
        int money = r.nextInt(difference + 1) + this.minCoins;
        return money;
    }
}
