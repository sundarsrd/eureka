package com.netflix.eureka.interests;

import com.netflix.eureka.registry.InstanceInfo;

/**
 * @author Nitesh Kant
 */
public class FullRegistryInterest extends Interest<InstanceInfo> {

    public static final FullRegistryInterest DEFAULT_INSTANCE = new FullRegistryInterest();

    private static final int HASH = 234234128;

    @Override
    public boolean matches(InstanceInfo data) {
        return true;
    }

    @Override
    public int hashCode() {
        return HASH;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof FullRegistryInterest;
    }

}
