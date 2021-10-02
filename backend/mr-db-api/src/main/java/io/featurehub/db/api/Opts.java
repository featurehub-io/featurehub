package io.featurehub.db.api;

import io.featurehub.db.FilterOpt;
import io.featurehub.db.FilterOptType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Opts {
  private Set<FillOpts> opts = null;
  private Set<FilterOpt> filterOpts = null;

  public Opts(@NotNull Set<FillOpts> opts) {
    this.opts = opts;
  }

  public Opts(@NotNull Set<FillOpts> opts, @NotNull Set<FilterOpt> filterOpts) {
    this.opts = opts;
    this.filterOpts = filterOpts;
  }

  public Opts() {}

  @NotNull
  public Opts add(FillOpts... optList) {
    if (opts == null) {
      opts = new HashSet<>();
    }

    opts.addAll(Arrays.asList(optList));

    return this;
  }

  @NotNull
  public Opts add(FilterOpt... optList) {
    if (filterOpts == null) {
      filterOpts = new HashSet<>();
    }

    filterOpts.addAll(Arrays.asList(optList));

    return this;
  }

  @NotNull
  public Opts add(@NotNull FillOpts opt, @Nullable Boolean bool) {
    if (Boolean.TRUE.equals(bool)) {
      return add(opt);
    }

    return this;
  }

  @NotNull
  public Opts add(FilterOptType opt, UUID id) {
    if (id != null) {
      return add(new FilterOpt(id, opt));
    }

    return this;
  }

  public boolean contains(FillOpts opt) {
    return opts != null && opts.contains(opt);
  }

  public boolean contains(FilterOptType opt) {
    return filterOpts != null && filterOpts.stream().anyMatch(ot -> ot.getFilter() == opt);
  }

  @Nullable
  public UUID id(FilterOptType opt) {
    if (filterOpts == null) {
      return null;
    }

    return filterOpts.stream()
        .filter(ot -> ot.getFilter() == opt)
        .findFirst()
        .map(FilterOpt::getId)
        .orElse(null);
  }

  @NotNull
  public Optional<Boolean> is(FillOpts opt) {
    return Optional.of(contains(opt));
  }

  @NotNull
  public static Opts empty() {
    return new Opts();
  }

  @NotNull
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
   *
   * @param minusOpt
   * @return the current opts minus the ones passed
   */
  @NotNull
  public Opts minus(FillOpts... minusOpt) {
    Set<FillOpts> newOpts = new HashSet<>(this.opts);
    newOpts.removeAll(Arrays.asList(minusOpt));
    return new Opts(newOpts);
  }
}
