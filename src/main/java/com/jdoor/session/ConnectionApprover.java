package com.jdoor.session;

@FunctionalInterface
public interface ConnectionApprover {
    boolean approve(ConnectionRequest request);
}
