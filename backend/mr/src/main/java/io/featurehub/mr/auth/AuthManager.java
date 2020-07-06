package io.featurehub.mr.auth;

import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.OrganizationApi;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.function.Consumer;

@Singleton
public class AuthManager implements AuthManagerService {
  private final GroupApi groupApi;
  private final PortfolioApi portfolioApi;

  @Inject
  public AuthManager(GroupApi groupApi, PortfolioApi portfolioApi) {
    this.groupApi = groupApi;
    this.portfolioApi = portfolioApi;
  }

  public boolean isPortfolioAdmin(String portfolioId, Person person, Consumer<Group> action) {
    if (person == null || portfolioId == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, person.getId(), action);
  }

  public boolean isPortfolioAdmin(Portfolio portfolio, Person person) {
    if (portfolio == null || person == null) {
      return false;
    }

    return isPortfolioAdmin(portfolio.getId(), person, null);
  }

  public boolean isPortfolioAdmin(String portfolioId, Person person) {
    if (portfolioId == null || person == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, person, null);
  }

  @Override
  public boolean isAnyAdmin(String personId) {
    return isOrgAdmin(personId) ||
      groupApi.groupsWherePersonIsAnAdminMember(personId).size() > 0;
  }

  @Override
  public boolean isAnyAdmin(Person personId) {
    return personId != null && isAnyAdmin(personId.getId().getId());
  }

  @Override
  public ServiceAccount serviceAccount(SecurityContext ctx) {
    return null;
  }

  @Override
  public boolean isPortfolioGroupMember(String id, Person from) {
    return groupApi.isPersonMemberOfPortfolioGroup(id, from.getId().getId());
  }

  public boolean isPortfolioAdmin(String portfolioId, PersonId personId, Consumer<Group> action) {
    if (personId == null || portfolioId == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, personId.getId(), action);
  }

  public boolean isPortfolioAdmin(String portfolioId, String personId, Consumer<Group> action) {
    if (personId == null || portfolioId == null) {
      return false;
    }

    Group adminGroup = null;

    boolean member = false;

    // this is a portfolio groupToCheck, so find the groupToCheck belonging to this portfolio
    adminGroup = groupApi.findPortfolioAdminGroup(portfolioId, Opts.opts(FillOpts.Members));
    if (adminGroup == null) { // no such portfolio
      return false;
    }
    String orgId = null;

    member = isGroupMember(personId, adminGroup);

    if (!member) {
      Portfolio p = portfolioApi.getPortfolio(portfolioId, Opts.empty(), new Person().id(new PersonId().id(personId)));

      if (p != null) {
        orgId = p.getOrganizationId();
      }
    }

    if (!member && orgId != null) {
      adminGroup = groupApi.findOrganizationAdminGroup(orgId, Opts.opts(FillOpts.Members));
      member = isGroupMember(personId, adminGroup);
    }

    if (member) {
      if (action != null) {
        action.accept(adminGroup);
      }

      return true;
    }

    return false;
  }

  private boolean isOrgAdmin(String personId) {
    return !groupApi.groupsPersonOrgAdminOf(personId).isEmpty();
  }

  @Override
  public boolean isOrgAdmin(Person person) {
    if (person == null || person.getId() == null || person.getId().getId() == null) {
      return false;
    }

    return isOrgAdmin(person.getId().getId());
  }

  public boolean isGroupMember(String userId, Group group) {
    return group.getMembers().stream().map(Person::getId).anyMatch(uid -> uid.getId().equals(userId));
  }


  public Person from(SecurityContext context) {
    return ((AuthHolder) context.getUserPrincipal()).getPerson();
  }

  @Override
  public String orgPersonIn(Person person) {
    return orgPersonIn(person.getId().getId());
  }

  @Override
  public String orgPersonIn(String id) {
    List<Organization> orgs = groupApi.orgsUserIn(id);
    if (orgs.isEmpty()) {
      return null;
    }
    return orgs.get(0).getId();
  }

  @Override
  public String orgPersonIn(PersonId personId) {
    return orgPersonIn(personId.getId());
  }
}
