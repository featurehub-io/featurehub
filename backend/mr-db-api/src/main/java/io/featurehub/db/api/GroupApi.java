package io.featurehub.db.api;

import io.featurehub.mr.model.Group;
import io.featurehub.mr.model.Person;
import io.featurehub.mr.model.SortOrder;

import java.util.List;

public interface GroupApi {

  boolean isPersonMemberOfPortfolioGroup(String portfolioId, String personId);

  Group getSuperuserGroup(String id, Person personAsking);

  class DuplicateGroupException extends Exception {}
  /**
   * Creates top level admin group for a given organization
   * @param orgId
   * @param groupName - group name
   * @return Group with the group id
   */
  Group createOrgAdminGroup(String orgId, String groupName, Person whoCreated);

  Group createPortfolioGroup(String portfolioId, Group group, Person whoCreated) throws DuplicateGroupException;

  /**
   * Adds a person to a group
   * @param groupId
   * @param personId
   * @param opts
   * @return Group with the group id - default. Or plus opts if provided
   */
  Group addPersonToGroup(String groupId, String personId, Opts opts);

  Group getGroup(String gid, Opts opts, Person person);

  Group findPortfolioAdminGroup(String portfolioId, Opts opts);

  Group findOrganizationAdminGroup(String orgId, Opts opts);

  List<Group> groupsWherePersonIsAnAdminMember(String personId);

  void deleteGroup(String gid);

  Group deletePersonFromGroup(String gid, String id, Opts opts);

  Group updateGroup(String gid, Group group, boolean updateMembers, boolean updateApplicationGroupRoles, boolean updateEnvironmentGroupRoles, Opts opts) throws OptimisticLockingException, DuplicateGroupException;

  List<Group> findGroups(String portfolioId, String filter, SortOrder order, Opts opts);

  void updateAdminGroupForPortfolio(String id, String name);
}
