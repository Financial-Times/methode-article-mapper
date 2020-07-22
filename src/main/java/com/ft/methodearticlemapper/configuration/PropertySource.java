package com.ft.methodearticlemapper.configuration;

/** Defines how a property should be sourced during mapping. */
public enum PropertySource {
  /** Property value should be taken from the native content supplied to the mapper. */
  fromNative,
  /** Property value should be taken from the current transaction or request context. */
  fromTransaction
}
