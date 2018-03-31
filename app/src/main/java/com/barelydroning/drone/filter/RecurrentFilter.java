package com.barelydroning.drone.filter;

import java.util.ArrayList;

/**
 * Created by axelri on 2018-03-31.
 */

class RecurrentFilter implements Filter {
    private final double alpha;
    private final ArrayList<Double> x = new ArrayList<>();
    private Double prevY = null;

    public RecurrentFilter(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public void addCurrentX(double value) {
        if (x.size() == 2) {
            x.set(0, x.get(1));
            x.set(1, value);
        } else {
            x.add(value);
        }
    }

    @Override
    public double getCurrentY() {
        if (x.isEmpty()) {
            throw new RuntimeException("No value go get");
        }

        if (x.size() < 2) {
            return x.get(0);
        }

        double filtered = x.get(0) + alpha * prevY;
        prevY = filtered;
        return filtered;
    }
}
