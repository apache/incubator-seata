package com.alibaba.fescar.metrics;

public interface Counter<T extends Number> extends Meter {
  T increase(T value);

  T decrease(T value);

  T get();
}
