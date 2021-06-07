package io.featurehub.db.api;

import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Organization;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;

import java.util.List;
import java.util.UUID;

public interface GroupApi {

  boolean isPersonMemberOfPortfolioGroup(UUID portfolioId, UUID personId);

  Group getSuperuserGroup(UUID id, Person personAsking);

  List<Group> groupsPersonOrgAdminOf(UUID personId);

  List<Organization> orgsUserIn(UUID personId);

  class DuplicateGroupException extends Exception {}
  /**
   * Creates top level admin group for a given organization
   * @param orgId
   * @param groupName - group name
   * @return Group with the group id
   */
  Group createOrgAdminGroup(UUID orgId, String groupName, Person whoCreated);

  Group createPortfolioGroup(UUID portfolioId, Group group, Person whoCreated) throws DuplicateGroupException;

  /**
   * Adds a person to a group
   * @param groupId
   * @param personId
   * @param opts
   * @return Group with the group id - default. Or plus opts if provided
   */
  Group addPersonToGroup(UUID groupId, UUID personId, Opts opts);

  Group getGroup(UUID gid, Opts opts, Person person);

  Group findPortfolioAdminGroup(UUID portfolioId, Opts opts);

  Group findOrganizationAdminGroup(UUID orgId, Opts opts);

  List<Group> groupsWherePersonIsAnAdminMember(UUID personId);

  void deleteGroup(UUID gid);

  Group deletePersonFromGroup(UUID gid, UUID id, Opts opts);

  Group updateGroup(UUID gid, Group group, boolean updateMembers, boolean updateApplicationGroupRoles, boolean updateEnvironmentGroupRoles, Opts opts)
    throws OptimisticLockingException, DuplicateGroupException, DuplicateUsersException;

  List<Group> findGroups(UUID portfolioId, String filter, SortOrder order, Opts opts);

  void updateAdminGroupForPortfolio(UUID id, String name);
}
