import * as pulumi from "@pulumi/pulumi";
import * as gcp from "@pulumi/gcp";

const config = new pulumi.Config()
const bucketRegion = config.get("bucketRegion");

const bucket = new gcp.storage.Bucket("featurehub-openapi-bucket", {
    name: "api.dev.featurehub.io",
    location: bucketRegion,
    forceDestroy: config.get("bucketDestroy") === "true",
});
const publicRule = new gcp.storage.DefaultObjectAccessControl("featurehub-openapi-bucket-rule", {
    bucket: bucket.name,
    role: "READER",
    entity: "allUsers",
});
const fhOwner = new gcp.storage.DefaultObjectAccessControl("featurehub-openapi-bucket-owner-rule", {
    bucket: bucket.name,
    role: "OWNER",
    entity: "allAuthenticatedUsers",
});

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
