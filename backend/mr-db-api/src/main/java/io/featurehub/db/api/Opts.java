package io.featurehub.db.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Opts {
  private Set<FillOpts> opts = null;

  public Opts(Set<FillOpts> opts) {
    this.opts = opts;
  }

  public Opts() {
  }

  public Opts add(FillOpts... optList) {
    if (opts == null) {
      opts = new HashSet<>();
    }

    opts.addAll(Arrays.asList(optList));

    return this;
  }

  public Opts add(FillOpts opt, Boolean bool) {
    if (Boolean.TRUE.equals(bool)) {
      return add(opt);
    }

    return this;
  }

  public boolean contains(FillOpts opt) {
    return opts != null && opts.contains(opt);
  }

  public Optional<Boolean> is(FillOpts opt) {
    return Optional.of(contains(opt));
  }

  public static Opts empty() {
    return new Opts();
  }

  public static Opts opts(FillOpts... opts) {
    return new Opts(Arrays.stream(opts).collect(Collectors.toSet()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Opts opts1 = (Opts) o;
    return Objects.equals(opts, opts1.opts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(opts);
  }

  /**
   * creates a copy of this Opt minus the specified one(s)
   * @param minusOpt
   * @return
   */
  public Opts minus(FillOpts ...minusOpt) {
    Set<FillOpts> newOpts = new HashSet<>(this.opts);
    newOpts.removeAll(Arrays.asList(minusOpt));
    return new Opts(newOpts);
  }
}
