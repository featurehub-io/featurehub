package io.featurehub.events.pubsub

import com.google.api.gax.core.NoCredentialsProvider
import com.google.api.gax.grpc.GrpcTransportChannel
import com.google.api.gax.rpc.FixedTransportChannelProvider
import com.google.cloud.pubsub.v1.SubscriptionAdminClient
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings
import com.google.cloud.pubsub.v1.TopicAdminClient
import com.google.cloud.pubsub.v1.TopicAdminSettings
import com.google.pubsub.v1.ProjectName
import io.grpc.ManagedChannelBuilder

class TopicLister {
  static void main(String[] args) {
    String pubsubHost = "localhost:8075"
    String projectId = "featurehub"

    def channel = ManagedChannelBuilder.forTarget(pubsubHost).usePlaintext().build()
    def channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel))
    def credsProvider = NoCredentialsProvider.create()

    def topicAdminClient = TopicAdminClient.create(
      TopicAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .setCredentialsProvider(credsProvider)
        .build()
    )

    def subscriptionAdminClient = SubscriptionAdminClient.create(
      SubscriptionAdminSettings.newBuilder()
        .setTransportChannelProvider(channelProvider)
        .setCredentialsProvider(credsProvider)
        .build())

    def topics = topicAdminClient.listTopics(ProjectName.of(projectId)).iterateAll().toList()

    println("Topics are ${topics}")
//    subscriptionAdminClient.deleteSubscription("projects/featurehub/subscriptions/featurehub-mr-dacha2-sub")

    def subscriptions = subscriptionAdminClient.listSubscriptions(ProjectName.of(projectId)).iterateAll().toList()

    println("Subscriptions are")
    subscriptions.each {
      println(" - ${it.name}")
    }

//    subscriptions.each { sub ->
//      subscriptionAdminClient.deleteSubscription(sub.name)
//    }
  }
}
