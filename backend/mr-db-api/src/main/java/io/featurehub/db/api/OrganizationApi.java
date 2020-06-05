package io.featurehub.db.api;
import io.featurehub.mr.model.Organization;


public interface OrganizationApi {

  /**
   *
   * @return the sole organisation if it exists
   */
//  DbOrganisation get();

  /**
   * saves or updates an existing organisation.
   *
   * @param organisation
   * @return updated or new organisation.
   */
  Organization save(Organization organisation);

  Organization get();
}
