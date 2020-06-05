package io.featurehub.mr.auth;

import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;

import javax.ws.rs.core.SecurityContext;
import java.util.function.Consumer;

public interface AuthManagerService {
  Person from(SecurityContext context);
  boolean isOrgAdmin(Person person);
  boolean isPortfolioAdmin(String portfolioId, String personId, Consumer<Group> action);
  boolean isPortfolioAdmin(String portfolioId, PersonId personId, Consumer<Group> action);
  boolean isPortfolioAdmin(String portfolioId, Person person, Consumer<Group> action);
  boolean isPortfolioAdmin(Portfolio portfolio, Person person);
//  boolean isApplicationMember(String id, Person person);
  boolean isAnyAdmin(String personId);
  boolean isAnyAdmin(Person personId);

  ServiceAccount serviceAccount(SecurityContext ctx);

  boolean isPortfolioGroupMember(String id, Person from);
}
