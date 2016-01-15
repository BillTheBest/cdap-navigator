/*
 * Copyright © 2016 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.metadata;

import co.cask.cdap.api.annotation.UseDataSet;
import co.cask.cdap.api.common.Bytes;
import co.cask.cdap.api.dataset.lib.KeyValueTable;
import co.cask.cdap.api.flow.flowlet.OutputEmitter;
import co.cask.cdap.kafka.flow.Kafka08ConsumerFlowlet;
import co.cask.cdap.kafka.flow.KafkaConfigurer;
import co.cask.cdap.kafka.flow.KafkaConsumerConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;

/**
 * Subscribes to Kafka messages published for the CDAP Platform that contains the Metadata Change records.
 */
public final class MetadataConsumer extends Kafka08ConsumerFlowlet<ByteBuffer, ByteBuffer> {

  private static final Logger LOG = LoggerFactory.getLogger(MetadataConsumer.class);

  // TODO: Add a way to reset the offset?

  @UseDataSet("kafkaOffsets")
  private KeyValueTable offsetStore;

  private OutputEmitter<String> emitter;

  @Override
  protected KeyValueTable getOffsetStore() {
    return offsetStore;
  }

  @Override
  protected void configureKafka(KafkaConfigurer kafkaConfigurer) {
    kafkaConfigurer.setZooKeeper(getContext().getRuntimeArguments().get("kafka.zookeeper"));
    setupTopicPartitions(kafkaConfigurer, getContext().getRuntimeArguments());
  }

  @Override
  protected void handleInstancesChanged(KafkaConsumerConfigurer configurer) {
    setupTopicPartitions(configurer, getContext().getRuntimeArguments());
  }

  private void setupTopicPartitions(KafkaConsumerConfigurer configurer, Map<String, String> runtimeArgs) {
    int partitions = Integer.parseInt(runtimeArgs.get("kafka.partitions"));
    int instanceId = getContext().getInstanceId();
    int instances = getContext().getInstanceCount();
    for (int i = 0; i < partitions; i++) {
      if ((i % instances) == instanceId) {
        configurer.addTopicPartition(runtimeArgs.get("kafka.topic"), i);
      }
    }
  }

  @Override
  protected void processMessage(ByteBuffer metadataKafkaMessage) throws Exception {
    emitter.emit(Bytes.toString(metadataKafkaMessage));
  }
}
