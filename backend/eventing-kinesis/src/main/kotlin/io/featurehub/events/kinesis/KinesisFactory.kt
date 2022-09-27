package io.featurehub.events.kinesis

import cd.connect.app.config.ConfigKey
import cd.connect.app.config.DeclaredConfigResolver
import io.cloudevents.CloudEvent
import io.cloudevents.jackson.JsonFormat
import io.cloudevents.kinesis.KinesisMessageFactory
import io.featurehub.health.HealthSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient
import software.amazon.kinesis.common.ConfigsBuilder
import software.amazon.kinesis.common.KinesisClientUtil
import software.amazon.kinesis.coordinator.Scheduler
import software.amazon.kinesis.lifecycle.events.*
import software.amazon.kinesis.processor.ShardRecordProcessor
import software.amazon.kinesis.processor.ShardRecordProcessorFactory
import software.amazon.kinesis.retrieval.polling.PollingConfig
import java.net.URI
import java.util.*
import java.util.concurrent.ExecutionException

interface KinesisCloudEventsPublisher {
  fun publish(msg: CloudEvent, partition: String)
}

interface KinesisFactory {
  /**
   * makeSubscriber - this causes us to subscriber to this stream as per KCL rules.
   *
   * applicationName: this should be different when you want pub/sub (i.e. everyone reads the stream), or the same if you want a queue
   * (everyone shares the stream). Conversely, you want the partitionkey to be the same if you need _ordering_ on the outgoing stream (which
   * you do on queues), and _different_ if you don't care about the order.
   *
   * streamName: this is the name of the outgoing stream
   *
   * messageHandler: handles the message, if it cannot be handled, it gets skipped.
   */
  fun makeSubscriber(applicationName: String, streamName: String, messageHandler: (message: CloudEvent) -> Unit)


  fun makePublisher(streamName: String): KinesisCloudEventsPublisher
}

class KinesisFactoryImpl : KinesisFactory, HealthSource {
  @ConfigKey("cloudevents.kinesis.aws.region")
  var awsRegion: String? = "us-east-1"
  @ConfigKey("cloudevents.kinesis.shutdown-length")
  var shutdownLength: Int? = 20
  // do we put synchronously
  @ConfigKey("cloudevents.kinesis.put-sync")
  var putSync: Boolean? = false

  @ConfigKey("cloudevents.kinesis.endpointUrl")
  var endpointOverride: String? = ""

  private val log: Logger = LoggerFactory.getLogger(KinesisFactoryImpl::class.java)
  private val awsCredentialsProvider: AwsCredentialsProvider = DefaultCredentialsProvider.create()
  private var region: Region?
  private val kinesisClient: KinesisAsyncClient
  private val subscribers = mutableListOf<ListenerTracker>()

  init {
    DeclaredConfigResolver.resolve(this)

    region = Region.of(awsRegion!!)

    val builder = KinesisAsyncClient.builder()
        .credentialsProvider(awsCredentialsProvider)
        .region(this.region)

    if (endpointOverride!!.isNotEmpty()) {
      builder.endpointOverride(URI.create(endpointOverride!!))
    }

    kinesisClient = KinesisClientUtil.createKinesisAsyncClient(builder)

    Runtime.getRuntime().addShutdownHook(Thread {
      log.info("Shutting down Kinesis listeners and waiting for {} seconds", shutdownLength)

      subscribers.forEach { tracker ->
        // good luck!
        tracker.scheduler?.startGracefulShutdown()
      }

      Thread.sleep(shutdownLength!! * 1000L)
    })
  }

  private fun makeDynamoClient(): DynamoDbAsyncClient {
    val builder = DynamoDbAsyncClient.builder().region(region)
      .credentialsProvider(awsCredentialsProvider)

    if (endpointOverride!!.isNotEmpty()) {
      builder.endpointOverride(URI.create(endpointOverride!!))
    }

    return builder.build()!!
  }

  private fun makeCloudwatchClient(): CloudWatchAsyncClient {
    val builder = CloudWatchAsyncClient.builder().region(region)
      .credentialsProvider(awsCredentialsProvider)

    if (endpointOverride!!.isNotEmpty()) {
      builder.endpointOverride(URI.create(endpointOverride!!))
    }

    return builder.build()!!
  }

  internal data class ListenerTracker(val applicationName: String, val streamName: String, var status: Boolean,
                                      val message: (message: CloudEvent) -> Unit, var scheduler: Scheduler?)

  internal class CloudEventRecordProcessor(private val tracker: ListenerTracker) : ShardRecordProcessor {
    private val log: Logger = LoggerFactory.getLogger(CloudEventRecordProcessor::class.java)

    override fun initialize(initializationInput: InitializationInput?) {
    }

    override fun processRecords(processRecordsInput: ProcessRecordsInput) {
      processRecordsInput.records().forEach {
        KinesisMessageFactory.createReader(it).toEvent()?.let { msg ->
          try {
            tracker.message(msg)
          } catch (e: Exception) {
            log.error("Unable to process cloud event, skipping {}", msg, e)
          }
        } ?: log.error("Unable to process incoming kinesis event {}", it.partitionKey())
      }
    }

    override fun leaseLost(leaseLostInput: LeaseLostInput?) {
      tracker.status = false  // make health check go fail
      log.warn("got lease lost on stream {}", tracker.streamName)
    }

    override fun shardEnded(shardEndedInput: ShardEndedInput?) {
    }

    override fun shutdownRequested(shutdownRequestedInput: ShutdownRequestedInput?) {
      log.warn("got shutdown request on stream {}", tracker.streamName)
      tracker.status = false // make health check go fail
    }
  }

  internal class CloudEventKinesisProcessorFactory(private val tracker: ListenerTracker) : ShardRecordProcessorFactory {
    override fun shardRecordProcessor(): ShardRecordProcessor {
      return CloudEventRecordProcessor(tracker)
    }
  }

  override fun makeSubscriber(applicationName: String, streamName: String, messageHandler: (message: CloudEvent) -> Unit) {
    val tracker = ListenerTracker(applicationName, streamName, true, messageHandler, null)

    log.info("kinesis: {} listening to {}", applicationName, streamName)

    val configsBuilder = ConfigsBuilder(
      streamName, applicationName, kinesisClient, makeDynamoClient(),
      makeCloudwatchClient(), UUID.randomUUID().toString(),
      CloudEventKinesisProcessorFactory(tracker)
    )

    val scheduler = Scheduler(
      configsBuilder.checkpointConfig(),
      configsBuilder.coordinatorConfig(),
      configsBuilder.leaseManagementConfig(),
      configsBuilder.lifecycleConfig(),
      configsBuilder.metricsConfig(),
      configsBuilder.processorConfig(),
      configsBuilder.retrievalConfig()
        .retrievalSpecificConfig(PollingConfig(streamName, kinesisClient))
    )

    tracker.scheduler = scheduler
    /*
         * Kickoff the Scheduler. Record processing of the stream of dummy data will continue indefinitely
         * until an exit is triggered.
         */

    /*
         * Kickoff the Scheduler. Record processing of the stream of dummy data will continue indefinitely
         * until an exit is triggered.
         */
    val schedulerThread = Thread(scheduler)
    schedulerThread.isDaemon = true
    schedulerThread.start()
  }

  val jsonFormat = JsonFormat()

  override fun makePublisher(streamName: String): KinesisCloudEventsPublisher  {
    return object: KinesisCloudEventsPublisher {
      override fun publish(msg: CloudEvent, partition: String) {
        try {
          log.info("kinesis: publishing {}", msg)
          val put = kinesisClient.putRecord(
            KinesisMessageFactory.createWriter().writeStructured(msg, jsonFormat)
              .partitionKey(partition)
              .streamName(streamName).build())

          if (putSync!!) {
            put.get()
          }
        } catch (e: InterruptedException) {
          log.error("Unable to output event on stream {}, assuming shutdown", streamName, e)
        } catch (e: ExecutionException) {
          log.error("Error when trying to send in background to Kinesis, will try later", e)
        }
      }

    }
  }

  override val healthy: Boolean
    get() = subscribers.all { it.status }

  override val sourceName: String
    get() = "kinesis-factory"
}
