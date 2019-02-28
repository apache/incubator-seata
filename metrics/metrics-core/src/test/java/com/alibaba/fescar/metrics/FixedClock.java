package com.alibaba.fescar.metrics;

public class FixedClock implements Clock {
  private final double[] values;

  private int index;

  public FixedClock(double... values) {
    this.values = values;
    this.index = 0;
  }

  //no lock because only for test
  @Override
  public double getCurrentMilliseconds() {
    if (index >= values.length) {
      throw new RuntimeException("out if index, max call = " + values.length);
    }

    double value = values[index];
    index++;
    return value;
  }
}
