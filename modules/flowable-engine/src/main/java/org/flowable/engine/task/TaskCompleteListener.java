/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.task;

import com.rabbitmq.client.impl.CredentialsProvider;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.ExecutionHelper;
import org.flowable.task.service.delegate.DelegateTask;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * @author Joram Barrez
 */
public class TaskCompleteListener implements TaskListener {

    private Expression queuesName;
    private Expression exchangeName;
    private Expression message;
    private Expression routingKey;

    @Override
    public void notify(DelegateTask delegateTask) {
       ExecutionEntity execution = ExecutionHelper.getExecution(delegateTask.getExecutionId());
//        execution.setVariable("greeting", "Hello from " + greeter.getValue(execution));
//        execution.setVariable("shortName", shortName.getValue(execution));
//
//        delegateTask.setVariableLocal("myTaskVariable", "test");
        Map<String, String> env = System.getenv();

        final String amqpHost = env.getOrDefault("AMQP_HOST" ,"localhost");
        final String amqpPort = env.getOrDefault("AMQP_PORT" ,"5672");
        final String amqpUserName = env.getOrDefault("AMQP_USERNAME" , "guest");
        final String amqpPassword = env.getOrDefault("AMQP_HOST" , "guest");

        System.out.println("AMQP Host: "+amqpHost);
        System.out.println("AMQP User name: "+amqpUserName);
        System.out.println("AMQP password: "+amqpPassword);

        String QUEUE_NAME = (String) queuesName.getValue(execution);
        String EXCHANGE_NAME = (String) exchangeName.getValue(execution);
        String ROUTING_KEY = (String) routingKey.getValue(execution);
        String MESSAGE = (String) message.getValue(execution);

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(amqpHost);
        factory.setUsername(amqpUserName);
        factory.setPassword(amqpPassword);
        factory.setPort(Integer.parseInt(amqpPort));

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);

            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, MESSAGE.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + ROUTING_KEY + "':'" + message + "'");
        }catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
