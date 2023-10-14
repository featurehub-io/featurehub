package io.featurehub.mr.auth;

import io.featurehub.db.api.EnvironmentApi;
import io.featurehub.db.api.FillOpts;
import io.featurehub.db.api.GroupApi;
import io.featurehub.db.api.Opts;
import io.featurehub.db.api.PortfolioApi;
import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.PersonId;
import io.featurehub.mr.model.Portfolio;
import io.featurehub.mr.model.ServiceAccount;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.core.SecurityContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Singleton
public class AuthManager implements AuthManagerService {
  private final GroupApi groupApi;
  private final PortfolioApi portfolioApi;
  private final EnvironmentApi environmentApi;

  @Inject
  public AuthManager(GroupApi groupApi, PortfolioApi portfolioApi, EnvironmentApi environmentApi) {
    this.groupApi = groupApi;
    this.portfolioApi = portfolioApi;
    this.environmentApi = environmentApi;
  }

  @Override
  public boolean isPortfolioAdmin(UUID portfolioId, Person person, Consumer<Group> action) {
    if (person == null || portfolioId == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, person.getId(), action);
  }

  @Override
  public boolean isPortfolioAdmin(Portfolio portfolio, Person person) {
    if (portfolio == null || person == null) {
      return false;
    }

    return isPortfolioAdmin(portfolio.getId(), person, null);
  }

  @Override
  public boolean isPortfolioAdmin(UUID portfolioId, Person person) {
    if (portfolioId == null || person == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, person, null);
  }

  @Override
  public boolean isAnyAdmin(UUID personId) {
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
  public boolean isPortfolioGroupMember(UUID id, Person from) {
    return groupApi.isPersonMemberOfPortfolioGroup(id, from.getId().getId());
  }

  @Override
  public boolean isPortfolioAdmin(UUID portfolioId, PersonId personId, Consumer<Group> action) {
    if (personId == null || portfolioId == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, personId.getId(), action);
  }

  public boolean isPortfolioAdmin(@NotNull UUID portfolioId, @NotNull UUID personId) {
    return isPortfolioAdmin(portfolioId, personId, null);
  }

  public boolean isPortfolioAdmin(@NotNull UUID portfolioId, @NotNull UUID personId, @Nullable Consumer<Group> action) {
    // this is a portfolio groupToCheck, so find the groupToCheck belonging to this portfolio
    Group adminGroup = groupApi.findPortfolioAdminGroup(portfolioId, Opts.opts(FillOpts.Members));
    if (adminGroup == null) { // no such portfolio
      return false;
    }
    UUID orgId = null;

    boolean member = isGroupMember(personId, adminGroup);

    if (!member) {
      Portfolio p = portfolioApi.getPortfolio(portfolioId, Opts.empty(), personId);

      if (p != null) {
        orgId = p.getOrganizationId();
      }
    }

    if (!member && orgId != null) {
      adminGroup = groupApi.findOrganizationAdminGroup(orgId, Opts.opts(FillOpts.Members));
      if (adminGroup != null) {
        member = isGroupMember(personId, adminGroup);
      }
    }

    if (member) {
      if (action != null) {
        action.accept(adminGroup);
      }

      return true;
    }

    return false;
  }

  public boolean isOrgAdmin(UUID personId) {
    return !groupApi.groupsPersonOrgAdminOf(personId).isEmpty();
  }

  @Override
  public boolean isOrgAdmin(Person person) {
    if (person == null || person.getId() == null || person.getId().getId() == null) {
      return false;
    }

    return isOrgAdmin(person.getId().getId());
  }

  public boolean isGroupMember(@NotNull UUID userId, @NotNull Group group) {
    return group.getMembers().stream().map(Person::getId).anyMatch(uid -> userId.equals(uid.getId()));
  }


  @Override
  public boolean isPortfolioAdminOfEnvironment(@NotNull UUID envId, @NotNull Person person) {
    final UUID portfolioId = environmentApi.portfolioEnvironmentBelongsTo(envId);

    if (portfolioId == null) {
      return false;
    }

    return isPortfolioAdmin(portfolioId, person);
  }

  @Override
  public Person from(SecurityContext context) {
    return ((AuthHolder) context.getUserPrincipal()).getPerson();
  }

  @Override
  public UUID who(SecurityContext context) {
    return from(context).getId().getId();
  }

  @Override
  public UUID orgPersonIn(Person person) {
    return orgPersonIn(person.getId().getId());
  }

  @Override
  public UUID orgPersonIn(UUID id) {
    List<Organization> orgs = groupApi.orgsUserIn(id);
    if (orgs.isEmpty()) {
      return null;
    }
    return orgs.get(0).getId();
  }

  @Override
  public UUID orgPersonIn(PersonId personId) {
    return orgPersonIn(personId.getId());
  }
}
