package io.featurehub.mr.auth;

import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;

import javax.ws.rs.core.SecurityContext;
import java.util.UUID;
import java.util.function.Consumer;

public interface AuthManagerService {
  Person from(SecurityContext context);
  UUID orgPersonIn(Person person);
  UUID orgPersonIn(UUID id);
  UUID orgPersonIn(PersonId personId);
  boolean isOrgAdmin(Person person);
  boolean isPortfolioAdmin(UUID portfolioId, UUID personId, Consumer<Group> action);
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
