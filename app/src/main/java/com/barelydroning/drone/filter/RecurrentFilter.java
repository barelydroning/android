package com.barelydroning.drone.filter;

import java.util.ArrayList;

/**
 * Created by axelri on 2018-03-31.
 */

public class RecurrentFilter implements Filter {
    private final double alpha;
    private final ArrayList<Double> x = new ArrayList<>();
    private final ArrayList<Double> y = new ArrayList<>();
    private Double prevY = null;

    public RecurrentFilter(double alpha) {
        this.alpha = alpha;
    }

    @Override
    public void addCurrentX(double value) {
        if (x.size() == 0) {
            x.add(value);
            y.add(value);
        } else if (x.size() == 1) {
            x.add(value);
            y.add(x.get(0) + alpha * y.get(0));
        } else {
            x.set(0, x.get(1));
            x.set(1, value);
            y.set(0, y.get(1));
            y.set(1, x.get(0) + alpha * y.get(0));
        }
    }

    @Override
    public double getCurrentY() {
        if (x.size() == 0) {
            throw new RuntimeException("No value to get");
        }

        if (y.size() < 2) {
            return x.get(x.size() - 1);
        }

        return y.get(1);
    }
}
