import kafka.server.KafkaConfig;
import kafka.server.KafkaRaftServer;
import kafka.tools.StorageTool;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.utils.Time;
import org.apache.kafka.metadata.properties.MetaProperties;

import java.nio.file.Files;
import java.util.HashMap;

import static org.apache.kafka.server.common.MetadataVersion.latestProduction;

public class OAuthBearerValidatorLeak {

    public static void main(String[] argv) throws Exception {

        var props = new HashMap<>();
        props.put("node.id", "1");
        props.put("process.roles", "broker,controller");
        props.put("controller.quorum.voters", "1@localhost:9093");
        props.put("listeners", "PLAINTEXT://:9092,CONTROLLER://:9093,EXTERNAL://:9094");
        props.put("inter.broker.listener.name", "EXTERNAL");
        props.put("advertised.listeners", "PLAINTEXT://localhost:9092,EXTERNAL://localhost:9094");
        props.put("controller.listener.names", "CONTROLLER");
        props.put("listener.security.protocol.map", "CONTROLLER:PLAINTEXT,EXTERNAL:PLAINTEXT,PLAINTEXT:SASL_PLAINTEXT");
        props.put("listener.name.plaintext.sasl.enabled.mechanisms", "OAUTHBEARER");
        props.put("listener.name.plaintext.oauthbearer.sasl.server.callback.handler.class", "org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler");
        props.put("listener.name.plaintext.oauthbearer.sasl.jaas.config", "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");


        props.put("listener.name.plaintext.sasl.oauthbearer.expected.audience", "default");
        props.put("listener.name.plaintext.sasl.oauthbearer.jwks.endpoint.url","http://localhost:8080/default/jwks");
        props.put("log.dirs", Files.createTempDirectory("kafka").toString());

        var config = new KafkaConfig(props);

        var directories = StorageTool.configToLogDirectories(config);

        var metaProps = new MetaProperties.Builder()
                .setNodeId(1)
                .setClusterId(Uuid.randomUuid().toString())
                .build();
        StorageTool.formatCommand(System.out, directories, metaProps, latestProduction(), true);

        var server = new KafkaRaftServer(config, Time.SYSTEM);
        server.startup();
        server.shutdown();
        server.awaitShutdown();

        var restarted = new KafkaRaftServer(config, Time.SYSTEM);
        restarted.startup();





    }
}
