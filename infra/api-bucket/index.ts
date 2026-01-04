import * as pulumi from "@pulumi/pulumi";
import * as gcp from "@pulumi/gcp";

const config = new pulumi.Config()
const bucketRegion = config.get("bucketRegion") || 'US-EAST1';

const apiDocsBucket = new gcp.storage.Bucket("featurehub-openapi-docs-bucket", {
  name: "featurehubapidocs",
  location: bucketRegion,
  forceDestroy: config.get("bucketDestroy") === "true",
});
const bucketPublicRule = new gcp.storage.DefaultObjectAccessControl("featurehub-openapi-docs-bucket-rule", {
  bucket: apiDocsBucket.name,
  role: "READER",
  entity: "allUsers",
});
const bucketFhOwner = new gcp.storage.DefaultObjectAccessControl("featurehub-openapi-docs-bucket-owner-rule", {
  bucket: apiDocsBucket.name,
  role: "OWNER",
  entity: "allAuthenticatedUsers",
});
