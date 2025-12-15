package com.evolutiongaming.prometheus.cassandra;

import java.util.concurrent.TimeUnit;

/* package */class Conversions {
    private static final long NS_IN_SEC = TimeUnit.SECONDS.toNanos(1);

    static double nsToSec(double ns) {
        return ns / NS_IN_SEC;
    }
}
