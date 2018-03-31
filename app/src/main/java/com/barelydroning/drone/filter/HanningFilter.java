package com.barelydroning.drone.filter;

import java.util.ArrayList;
import java.util.List;

class HanningFilter implements Filter {
    // higher index = newer
    private final List<Double> x = new ArrayList<>();

    public HanningFilter() {
    }

    @Override
    public void addCurrentX(double value) {
        if (x.size() == 3) {
            // shift array right
            x.set(0, x.get(1));
            x.set(1, x.get(2));
            x.set(2, value);
        } else {
            x.add(value);
        }
    }

    @Override
    public double getCurrentY() {
        if (x.isEmpty()) {
            throw new RuntimeException("No value go get");
        }

        if (x.size() < 3) {
            return x.get(x.size() - 1);
        }

        return 0.25 * (x.get(2) + 2 * x.get(1) + x.get(0));
    }
}
