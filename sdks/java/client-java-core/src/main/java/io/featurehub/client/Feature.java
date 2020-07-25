package io.featurehub.client;

public interface Feature {
  // this is the feature's KEY (or ALIAS). We use name() because in Java, this is idiomatic with an enum
  String name();
}
