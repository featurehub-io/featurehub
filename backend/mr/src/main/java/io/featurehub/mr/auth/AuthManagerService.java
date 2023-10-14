package io.featurehub.mr.auth;

import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;
import jakarta.ws.rs.core.SecurityContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public interface AuthManagerService {
  @NotNull Person from(SecurityContext context);
  @NotNull UUID who(SecurityContext context);
  UUID orgPersonIn(Person person);
  UUID orgPersonIn(UUID id);
  UUID orgPersonIn(PersonId personId);
  boolean isOrgAdmin(Person person);
  boolean isOrgAdmin(UUID person);
  boolean isPortfolioAdminOfEnvironment(@NotNull UUID envId, @NotNull Person person);
  boolean isPortfolioAdmin(@NotNull UUID portfolioId, @NotNull UUID personId, @Nullable Consumer<Group> action);
  boolean isPortfolioAdmin(@NotNull UUID portfolioId, @NotNull UUID personId);
  boolean isPortfolioAdmin(UUID portfolioId, PersonId personId, Consumer<Group> action);
  boolean isPortfolioAdmin(UUID portfolioId, Person person, Consumer<Group> action);
  boolean isPortfolioAdmin(Portfolio portfolio, Person person);
  boolean isPortfolioAdmin(UUID portfolio, Person person);
//  boolean isApplicationMember(String id, Person person);
  boolean isAnyAdmin(UUID personId);
  boolean isAnyAdmin(Person personId);

  ServiceAccount serviceAccount(SecurityContext ctx);

  boolean isPortfolioGroupMember(UUID id, Person from);
}
