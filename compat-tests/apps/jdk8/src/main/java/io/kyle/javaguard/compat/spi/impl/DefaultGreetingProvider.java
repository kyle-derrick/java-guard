package io.kyle.javaguard.compat.spi.impl;
import io.kyle.javaguard.compat.spi.GreetingProvider;
public class DefaultGreetingProvider implements GreetingProvider { public String greeting() { return "service-loader"; } }
