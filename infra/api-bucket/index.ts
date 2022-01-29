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
